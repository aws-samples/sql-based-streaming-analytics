import * as cdk from 'aws-cdk-lib';
import {Construct} from 'constructs';
import * as elasticbeanstalk from 'aws-cdk-lib/aws-elasticbeanstalk';
import * as s3assets from 'aws-cdk-lib/aws-s3-assets';
import * as iam from 'aws-cdk-lib/aws-iam'
import * as https from "https";
import * as fs from "fs";
import {Stream} from 'aws-cdk-lib/aws-kinesis'
import Func = jest.Func;

interface SqlBasedStreamingAnalyticsElasticBeanstalkStackProps extends cdk.StackProps {
    inputStream: Stream
    outputStream: Stream
}

export class SqlBasedStreamingAnalyticsElasticBeanstalkStack extends cdk.Stack {

    constructor(scope: Construct, id: string, props: SqlBasedStreamingAnalyticsElasticBeanstalkStackProps) {
        super(scope, id, props);
    }

    public async startResourceCreation() {
        console.log("Starting download...")
        await this.fileDownload();
        console.log("File downloaded...")
        const applicationJar = new s3assets.Asset(this, 'ApplicationJAR', {
            path: '/tmp/app.jar',
        });
        const appName = 'SqlBasedStreamingAnalyticsDataAccessUi';
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
        appVersionProps.addDependency(app);

        const myRole = new iam.Role(this, `${appName}-aws-elasticbeanstalk-ec2-role`, {
            assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
        });

        const managedPolicy = iam.ManagedPolicy.fromAwsManagedPolicyName('AWSElasticBeanstalkWebTier')
        myRole.addManagedPolicy(managedPolicy);

        let cfnInstanceProfile = new iam.CfnInstanceProfile(this, `${appName}-InstanceProfile`, {
            roles: [
                myRole.roleName
            ]
        });

        const optionSettingProperties: elasticbeanstalk.CfnEnvironment.OptionSettingProperty[] = [
            {
                namespace: 'aws:autoscaling:launchconfiguration',
                optionName: 'IamInstanceProfile',
                value: cfnInstanceProfile.instanceProfileName,
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
            }
        ];
        const elbEnv = new elasticbeanstalk.CfnEnvironment(this, 'Environment', {
            applicationName: app.applicationName || appName,
            solutionStackName: '64bit Amazon Linux 2023 v4.0.0 running Corretto 17',
            optionSettings: optionSettingProperties,
            versionLabel: appVersionProps.ref,
        });
    }

    private async fileDownload(): Promise<void> {
        const file = fs.createWriteStream('/tmp/app.jar')
        return new Promise((resolve, reject) => {
            https.get("https://github.com/JWThewes/reusable-asset-sql-based-streaming-analytics/suites/15647102284/artifacts/893063792", res => {
                console.log(res.statusMessage)
                console.log(res.statusCode)
                if (res.statusCode != 200) {
                    reject()
                }
                res.pipe(file)
                resolve()
            });
        })
    }
}