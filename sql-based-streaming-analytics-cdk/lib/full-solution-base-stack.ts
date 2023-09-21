
import * as cdk from 'aws-cdk-lib';
import {RemovalPolicy, Stack, Tags} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {Stream, StreamMode} from "aws-cdk-lib/aws-kinesis";
import * as kda from "@aws-cdk/aws-kinesisanalytics-flink-alpha";
import {Application} from "@aws-cdk/aws-kinesisanalytics-flink-alpha";
import {Bucket} from "aws-cdk-lib/aws-s3";
import {BucketDeployment, Source} from "aws-cdk-lib/aws-s3-deployment";
import * as fr from "follow-redirects";
import fs from "fs";

export class FullSolutionBaseStack extends cdk.Stack {

    public kinesisInputStream: Stream
    public kinesisOutputStream: Stream

    constructor(scope: Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);
        this.kinesisInputStream = this.createKinesisStream("inputStream");
        this.kinesisOutputStream = this.createKinesisStream("outputStream");
    }

    private createKinesisStream(streamName: string) {
        return new Stream(this, streamName, {
            streamMode: StreamMode.PROVISIONED,
            shardCount: 4,
        })
    }

}
