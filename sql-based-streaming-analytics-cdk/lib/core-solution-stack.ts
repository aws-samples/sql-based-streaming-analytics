// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import * as cdk from 'aws-cdk-lib';
import { RemovalPolicy, Stack, Tags } from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { Stream } from 'aws-cdk-lib/aws-kinesis';
import * as kda from '@aws-cdk/aws-kinesisanalytics-flink-alpha';
import { Application } from '@aws-cdk/aws-kinesisanalytics-flink-alpha';
import { Bucket, EventType } from 'aws-cdk-lib/aws-s3';
import { BucketDeployment, Source } from 'aws-cdk-lib/aws-s3-deployment';
import * as fr from 'follow-redirects';
import fs from 'fs';
import { NodejsFunction } from 'aws-cdk-lib/aws-lambda-nodejs';
import { S3EventSource } from 'aws-cdk-lib/aws-lambda-event-sources';
import * as iam from 'aws-cdk-lib/aws-iam';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';

interface CoreSolutionStackProps extends cdk.StackProps {
  inputStream?: Stream;
  outputStream?: Stream;
}

export class CoreSolutionStack extends cdk.Stack {
  public msfApplications: Application[] = [];
  private props: CoreSolutionStackProps;

  constructor(scope: Construct, id: string, props: CoreSolutionStackProps) {
    super(scope, id, props);
    this.props = props;
  }

  async startResourceCreation() {
    await this.fileDownload();
    const bucket = this.createSqlFileBucket();
    const bucketDeployment = this.uploadSqlFiles(bucket);
    fs.readdir(`${__dirname}/../../sql`, (err, files) => {
      if (err) {
        return console.log('Unable to scan directory: ' + err);
      }
      for (let file of files) {
        const s3Url = `s3://${bucket.bucketName}/${file}`;
        this.createMsfApplication(file.replace('.sql', ''), s3Url, bucket);
      }
    });
    const lambda = this.createRestartMsfApplicationLambda();
    this.msfApplications.forEach((app) => {
      lambda.node.addDependency(app);
      lambda.node.addDependency(bucketDeployment);
    });

    lambda.addEventSource(
      new S3EventSource(bucket, {
        events: [EventType.OBJECT_CREATED],
        filters: [
          {
            suffix: '.sql',
          },
        ],
      })
    );
    lambda.addToRolePolicy(
      new iam.PolicyStatement({
        actions: ['kinesisanalytics:ListApplications'],
        resources: ['*'],
        effect: iam.Effect.ALLOW,
      })
    );
    lambda.addToRolePolicy(
      new iam.PolicyStatement({
        actions: ['kinesisanalytics:UpdateApplication', 'kinesisanalytics:DescribeApplication'],
        resources: ['*'],
        effect: iam.Effect.ALLOW,
      })
    );
  }

  private createMsfApplication(sqlFile: string, s3Url: string, sqlFileBucket: Bucket) {
    let propertyGroups = this.createPropertyGroups(s3Url);

    const application = new kda.Application(this, sqlFile + 'MsfApplication', {
      code: kda.ApplicationCode.fromAsset(`${__dirname}/../flinkJob.jar`),
      runtime: kda.Runtime.FLINK_1_15,
      propertyGroups: propertyGroups,
      snapshotsEnabled: false,
      parallelismPerKpu: 1,
      removalPolicy: RemovalPolicy.DESTROY,
    });
    if (this.props.inputStream) {
      this.props.inputStream.grantReadWrite(application);
    }
    if (this.props.outputStream) {
      this.props.outputStream.grantReadWrite(application);
    }
    Tags.of(application).add('application', 'sqlBasedStreamingAnalytics');
    this.msfApplications.push(application);
    sqlFileBucket.grantRead(application);
  }

  private createPropertyGroups(s3Url: string) {
    let propertyGroups: any = {
      ENV: {
        'run.file': s3Url,
        REGION: Stack.of(this).region,
      },
    };
    if (this.props.inputStream) {
      propertyGroups.ENV.INPUT_STREAM_NAME = this.props.inputStream.streamName;
    }
    if (this.props.outputStream) {
      propertyGroups.ENV.OUTPUT_STREAM_NAME = this.props.outputStream.streamName;
    }
    return propertyGroups;
  }

  private createSqlFileBucket() {
    return new Bucket(this, 'sqlFileBucket', {
      autoDeleteObjects: true,
      removalPolicy: RemovalPolicy.DESTROY,
    });
  }

  private uploadSqlFiles(bucket: Bucket) {
    return new BucketDeployment(this, 'SqlFileDeployment', {
      sources: [Source.asset(`${__dirname}/../../sql`)],
      destinationBucket: bucket,
    });
  }

  private async fileDownload(): Promise<void> {
    return new Promise((resolve, reject) => {
      fr.https.get(
        'https://github.com/JWThewes/release/releases/download/SNAPSHOT/sql-based-streaming-analytics-flink-job.jar',
        (res) => {
          if (res.statusCode != 200) {
            reject();
          }
          const writeStream = fs.createWriteStream(`${__dirname}/../flinkJob.jar`);
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

  private createRestartMsfApplicationLambda() {
    let nodejsFunction = new NodejsFunction(this, 'restartMsfApplicationLambda');
    new LogGroup(this, 'restart-msf-lambda-log-group', {
      logGroupName: `/aws/lambda/${nodejsFunction.functionName}`,
      removalPolicy: RemovalPolicy.DESTROY,
      retention: RetentionDays.ONE_WEEK,
    });
    return nodejsFunction;
  }
}
