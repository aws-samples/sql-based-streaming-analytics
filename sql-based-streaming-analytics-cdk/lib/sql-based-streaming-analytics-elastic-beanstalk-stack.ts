import * as cdk from 'aws-cdk-lib';
import {Construct} from 'constructs';
import * as elasticbeanstalk from 'aws-cdk-lib/aws-elasticbeanstalk';
import * as s3assets from 'aws-cdk-lib/aws-s3-assets';
import * as iam from 'aws-cdk-lib/aws-iam'
import * as fr from "follow-redirects"
import * as fs from "fs";
import {Stream} from 'aws-cdk-lib/aws-kinesis'
import Func = jest.Func;

interface SqlBasedStreamingAnalyticsElasticBeanstalkStackProps extends cdk.StackProps {
    inputStream: Stream
    outputStream: Stream
}

export class SqlBasedStreamingAnalyticsElasticBeanstalkStack extends cdk.Stack {
    private props: SqlBasedStreamingAnalyticsElasticBeanstalkStackProps;

    constructor(scope: Construct, id: string, props: SqlBasedStreamingAnalyticsElasticBeanstalkStackProps) {
        super(scope, id, props);
        this.props = props;
    }

    public async startResourceCreation() {
        await this.fileDownload();
        const applicationJar = new s3assets.Asset(this, 'ApplicationJAR', {
            path: `${__dirname}/../app.jar`,
        });
        const appName = 'DataAccessUi';
        const app = new elasticbeanstalk.CfnApplication(this, appName, {
            applicationName: appName,
        });
        const appVersionProps = new elasticbeanstalk.CfnApplicationVersion(this, 'AppVersion', {
            applicationName: appName,
            sourceBundle: {
                s3Bucket: applicationJar.s3BucketName,
                s3Key: applicationJar.s3ObjectKey,
            },
        });
        appVersionProps.addDependency(app)
        const ebIamRole = new iam.Role(this, `${appName}-aws-elasticbeanstalk-ec2-role`, {
            assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
        });
        ebIamRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('AWSElasticBeanstalkWebTier'));
        // this.props.inputStream.grantReadWrite(ebIamRole);
        // this.props.outputStream.grantReadWrite(ebIamRole);
        let instanceProfileName = `${appName}-InstanceProfile`;
        let cfnInstanceProfile = new iam.CfnInstanceProfile(this, instanceProfileName, {
            instanceProfileName: instanceProfileName,
            roles: [
                ebIamRole.roleName
            ]
        });
        const optionSettingProperties = this.createAppOptionSettings(instanceProfileName);
        let cfnEnvironment = new elasticbeanstalk.CfnEnvironment(this, 'Environment', {
            applicationName: app.applicationName || appName,
            solutionStackName: '64bit Amazon Linux 2023 v4.0.0 running Corretto 17',
            optionSettings: optionSettingProperties,
            versionLabel: appVersionProps.ref,
        });
        cfnEnvironment.addDependency(cfnInstanceProfile)
    }

    private createAppOptionSettings(instanceProfileName?: string) {
        const optionSettingProperties: elasticbeanstalk.CfnEnvironment.OptionSettingProperty[] = [
            {
                namespace: 'aws:autoscaling:launchconfiguration',
                optionName: 'IamInstanceProfile',
                value: instanceProfileName,
            },
            {
                namespace: 'aws:elasticbeanstalk:environment',
                optionName: 'EnvironmentType',
                value: 'SingleInstance',
            },
            {
                namespace: 'aws:ec2:instances',
                optionName: 'InstanceTypes',
                value: 't4g.large',
            },
            {
                namespace: "aws:elasticbeanstalk:application:environment",
                optionName: "SERVER_PORT",
                value: "5000"
            },
            {
                namespace: "aws:elasticbeanstalk:application:environment",
                optionName: "DATA_ACCESS_UI_INPUT_STREAM_NAME",
                value: this.props.inputStream.streamName
            },
            {
                namespace: "aws:elasticbeanstalk:application:environment",
                optionName: "DATA_ACCESS_UI_OUTPUT_STREAM_NAME",
                value: this.props.inputStream.streamName
            }
        ];
        return optionSettingProperties;
    }

    private async fileDownload(): Promise<void> {
        return new Promise((resolve, reject) => {
            fr.https.get("https://github.com/JWThewes/release/releases/download/SNAPSHOT/sql-based-streaming-analytics-data-access-ui.jar", res => {
                if (res.statusCode != 200) {
                    reject()
                }
                const writeStream = fs.createWriteStream(`${__dirname}/../app.jar`)
                writeStream.on("finish", () => {
                    writeStream.close();
                    resolve();
                }).on("error", reject);
                res.pipe(writeStream)
            });
        })
    }
}