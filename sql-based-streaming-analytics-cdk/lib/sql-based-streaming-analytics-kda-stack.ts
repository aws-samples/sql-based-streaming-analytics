import * as cdk from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {Stream, StreamMode} from "aws-cdk-lib/aws-kinesis";
import * as apprunner from "@aws-cdk/aws-apprunner-alpha";
import * as assets from "aws-cdk-lib/aws-ecr-assets";
import path from "path";

export class SqlBasedStreamingAnalyticsKdaStack extends cdk.Stack {
    constructor(scope: Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);
        let kinesisInputStream = this.createKinesisStream("inputStream");
        let kinesisOutputStream = this.createKinesisStream("outputStream");
        let appRunner = this.createAppRunner();
        kinesisInputStream.grantReadWrite(appRunner);
        kinesisOutputStream.grantReadWrite(appRunner);
    }

    private createKinesisStream(streamName: string) {
        return new Stream(this, streamName, {
            streamMode: StreamMode.PROVISIONED,
            shardCount: 4,
        })
    }

    private createAppRunner() {
        return new apprunner.Service(this, "AppService", {
            source: apprunner.Source.fromAsset({
                imageConfiguration: {port: 8080},
                asset: new assets.DockerImageAsset(this, "ImageAssets", {
                    directory: path.join(__dirname, "..", "..", "sql-based-streaming-analytics-data-access-ui"),
                    platform: assets.Platform.LINUX_AMD64,
                }),
            }),
        });
    }
}
