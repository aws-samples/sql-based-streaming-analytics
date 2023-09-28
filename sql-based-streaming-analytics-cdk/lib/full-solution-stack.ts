// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import * as cdk from 'aws-cdk-lib';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as elasticbeanstalk from 'aws-cdk-lib/aws-elasticbeanstalk';
import * as s3assets from 'aws-cdk-lib/aws-s3-assets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as fr from 'follow-redirects';
import * as fs from 'fs';
import { Stream } from 'aws-cdk-lib/aws-kinesis';
import { AttributeType, Table } from 'aws-cdk-lib/aws-dynamodb';
import { Application } from '@aws-cdk/aws-kinesisanalytics-flink-alpha';
import { IpAddresses, IVpc, SubnetType, Vpc } from 'aws-cdk-lib/aws-ec2';

interface FullSolutionProps extends cdk.StackProps {
  inputStream: Stream;
  outputStream: Stream;
  msfApplications: Application[];
}

export class FullSolutionStack extends cdk.Stack {
  private props: FullSolutionProps;

  constructor(scope: Construct, id: string, props: FullSolutionProps) {
    super(scope, id, props);
    this.props = props;
  }

  public async startResourceCreation() {
    await this.fileDownload();
    const kclCheckpointDynamoTable = this.createKclDynamoDbCheckpointTable();
    this.createElasticBeanstalkAppAndEnvironment(
      kclCheckpointDynamoTable,
      this.props.msfApplications
    );
  }

  private createElasticBeanstalkAppAndEnvironment(
    kclCheckpointDynamoTable: Table,
    msfApplications: Application[]
  ) {
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
    appVersionProps.addDependency(app);
    const ebIamRole = new iam.Role(this, `${appName}-aws-elasticbeanstalk-ec2-role`, {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
    });
    ebIamRole.addManagedPolicy(
      iam.ManagedPolicy.fromAwsManagedPolicyName('AWSElasticBeanstalkWebTier')
    );
    this.props.inputStream.grantReadWrite(ebIamRole);
    this.props.outputStream.grantReadWrite(ebIamRole);
    kclCheckpointDynamoTable.grantFullAccess(ebIamRole);
    ebIamRole.addToPrincipalPolicy(
      new iam.PolicyStatement({
        actions: ['cloudwatch:PutMetricData'],
        resources: ['*'],
        effect: iam.Effect.ALLOW,
      })
    );
    const applicationArns = msfApplications.map((app) => app.applicationArn);
    ebIamRole.addToPrincipalPolicy(
      new iam.PolicyStatement({
        actions: [
          'kinesisanalytics:ListTagsForResource',
          'kinesisanalytics:StopApplication',
          'kinesisanalytics:GetApplicationState',
          'kinesisanalytics:DescribeApplication',
          'kinesisanalytics:StartApplication',
        ],
        resources: applicationArns,
        effect: iam.Effect.ALLOW,
      })
    );
    ebIamRole.addToPrincipalPolicy(
      new iam.PolicyStatement({
        actions: ['kinesisanalytics:ListApplications'],
        resources: ['*'],
        effect: iam.Effect.ALLOW,
      })
    );
    let instanceProfileName = `${appName}-InstanceProfile`;
    let cfnInstanceProfile = new iam.CfnInstanceProfile(this, instanceProfileName, {
      instanceProfileName: instanceProfileName,
      roles: [ebIamRole.roleName],
    });
    const vpc = this.createDataAccessUiVpcAndSubnets();
    const optionSettingProperties = this.createAppOptionSettings(
      kclCheckpointDynamoTable,
      vpc,
      instanceProfileName
    );
    let cfnEnvironment = new elasticbeanstalk.CfnEnvironment(this, 'Environment', {
      applicationName: app.applicationName || appName,
      solutionStackName: '64bit Amazon Linux 2023 v4.0.1 running Corretto 17',
      optionSettings: optionSettingProperties,
      versionLabel: appVersionProps.ref,
    });
    cfnEnvironment.addDependency(cfnInstanceProfile);
    new cdk.CfnOutput(this, 'dataAccessUiUrl', {
      value: 'http://' + cfnEnvironment.attrEndpointUrl,
      description: 'The URL which can be used to access the Data Access UI',
      exportName: 'dataAccessUiUrl',
    });
  }

  private createAppOptionSettings(
    kclCheckpointDynmaoTable: Table,
    vpc: IVpc,
    instanceProfileName?: string
  ) {
    const optionSettingProperties: elasticbeanstalk.CfnEnvironment.OptionSettingProperty[] = [
      {
        namespace: 'aws:ec2:vpc',
        optionName: 'VPCId',
        value: vpc.vpcId,
      },
      {
        namespace: 'aws:ec2:vpc',
        optionName: 'ELBSubnets',
        value: vpc.selectSubnets({ subnetType: SubnetType.PUBLIC }).subnetIds.join(','),
      },
      {
        namespace: 'aws:ec2:vpc',
        optionName: 'Subnets',
        value: vpc
          .selectSubnets({ subnetType: SubnetType.PRIVATE_WITH_EGRESS })
          .subnetIds.join(','),
      },
      {
        namespace: 'aws:autoscaling:launchconfiguration',
        optionName: 'IamInstanceProfile',
        value: instanceProfileName,
      },
      {
        namespace: 'aws:elasticbeanstalk:environment',
        optionName: 'EnvironmentType',
        value: 'LoadBalanced',
      },
      {
        namespace: 'aws:elasticbeanstalk:environment',
        optionName: 'LoadBalancerType',
        value: 'application',
      },
      {
        namespace: 'aws:autoscaling:asg',
        optionName: 'MinSize',
        value: '1',
      },
      {
        namespace: 'aws:autoscaling:asg',
        optionName: 'MaxSize',
        value: '1',
      },
      {
        namespace: 'aws:ec2:instances',
        optionName: 'InstanceTypes',
        value: 't4g.large',
      },
      {
        namespace: 'aws:elasticbeanstalk:application:environment',
        optionName: 'SERVER_PORT',
        value: '5000',
      },
      {
        namespace: 'aws:elasticbeanstalk:application:environment',
        optionName: 'DATA_ACCESS_UI_INPUT_STREAM_NAME',
        value: this.props.inputStream.streamName,
      },
      {
        namespace: 'aws:elasticbeanstalk:application:environment',
        optionName: 'DATA_ACCESS_UI_OUTPUT_STREAM_NAME',
        value: this.props.outputStream.streamName,
      },
      {
        namespace: 'aws:elasticbeanstalk:application:environment',
        optionName: 'DATA_ACCESS_UI_KCL_CHECKPOINT_DYNAMO_TABLE_NAME',
        value: kclCheckpointDynmaoTable.tableName,
      },
    ];
    return optionSettingProperties;
  }

  private async fileDownload(): Promise<void> {
    return new Promise((resolve, reject) => {
      fr.https.get(
        'https://github.com/JWThewes/release/releases/download/SNAPSHOT/sql-based-streaming-analytics-data-access-ui.jar',
        (res) => {
          if (res.statusCode != 200) {
            reject();
          }
          const writeStream = fs.createWriteStream(`${__dirname}/../app.jar`);
          writeStream
            .on('finish', () => {
              writeStream.close();
              resolve();
            })
            .on('error', reject);
          res.pipe(writeStream);
        }
      );
    });
  }

  private createKclDynamoDbCheckpointTable() {
    return new Table(this, 'KclCheckpoint', {
      partitionKey: {
        name: 'leaseKey',
        type: AttributeType.STRING,
      },
      removalPolicy: RemovalPolicy.DESTROY,
    });
  }

  private createDataAccessUiVpcAndSubnets() {
    return new Vpc(this, 'DataAccessUiVpc', {
      maxAzs: 2,
      enableDnsHostnames: true,
      enableDnsSupport: true,
      ipAddresses: IpAddresses.cidr('10.0.0.0/16'),
      natGateways: 1,
      subnetConfiguration: [
        {
          cidrMask: 24,
          subnetType: SubnetType.PUBLIC,
          name: 'DataAccessUiPublicSubnet',
        },
        {
          cidrMask: 24,
          subnetType: SubnetType.PRIVATE_WITH_EGRESS,
          name: 'DataAccessUiPrivateSubnet',
        },
      ],
    });
  }
}
