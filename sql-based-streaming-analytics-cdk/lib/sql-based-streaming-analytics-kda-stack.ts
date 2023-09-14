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

export class SqlBasedStreamingAnalyticsKdaStack extends cdk.Stack {

    public kinesisInputStream: Stream
    public kinesisOutputStream: Stream
    public msfApplications: Application[] = []

    constructor(scope: Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);
    }

    async startResourceCreation() {
        await this.fileDownload()
        this.kinesisInputStream = this.createKinesisStream("inputStream");
        this.kinesisOutputStream = this.createKinesisStream("outputStream");
        const bucket = this.createSqlFileBucket();
        this.uploadSqlFiles(bucket)
        fs.readdir(`${__dirname}/../../sql`, (err, files) => {
            if (err) {
                return console.log('Unable to scan directory: ' + err);
            }
            for (let file of files) {
                console.log("Found file " + file);
                const s3Url = `s3://${bucket.bucketName}/${file}`
                this.createMsfApplication(file.replace(".sql", ""), s3Url, bucket);
            }
        });
        //
        // const s3Url = this.uploadSqlFile(bucket, "simpleSql.sql")
        // this.createMsfApplication("simpleSql", s3Url, bucket);
    }

    private createKinesisStream(streamName: string) {
        return new Stream(this, streamName, {
            streamMode: StreamMode.PROVISIONED,
            shardCount: 4,
        })
    }

    private createMsfApplication(sqlFile: string, s3Url: string, sqlFileBucket: Bucket) {
        console.log("SqlFileName " + sqlFile)
        let propertyGroups: any = {
            "stream": {
                "input.name": this.kinesisInputStream.streamName,
                "output.name": this.kinesisOutputStream.streamName,
            },
            "ENV": {
                "run.file": s3Url,
                "REGION": Stack.of(this).region,
                "INPUT_STREAM_NAME": this.kinesisInputStream.streamName,
                "OUTPUT_STREAM_NAME": this.kinesisOutputStream.streamName,
            }
        };

        const application = new kda.Application(this, sqlFile + "MsfApplication", {
                code: kda.ApplicationCode.fromAsset(`${__dirname}/../flinkJob.jar`),
                runtime: kda.Runtime.FLINK_1_15,
                propertyGroups: propertyGroups,
                snapshotsEnabled: false,
                parallelismPerKpu: 1,
                removalPolicy: RemovalPolicy.DESTROY
            }
        );
        Tags.of(application).add("application", "sqlBasedStreamingAnalytics");
        this.msfApplications.push(application)
        this.kinesisInputStream.grantReadWrite(application)
        this.kinesisOutputStream.grantReadWrite(application)
        sqlFileBucket.grantRead(application)
    }

    private createSqlFileBucket() {
        return new Bucket(this, "sqlFileBucket", {
            autoDeleteObjects: true,
            removalPolicy: RemovalPolicy.DESTROY
        })
    }

    private uploadSqlFiles(bucket: Bucket) {
        new BucketDeployment(this, "SqlFileDeployment", {
            sources: [Source.asset(`${__dirname}/../../sql`)],
            destinationBucket: bucket
        });
    }

    private async fileDownload(): Promise<void> {
        return new Promise((resolve, reject) => {
            fr.https.get("https://github.com/JWThewes/release/releases/download/SNAPSHOT/sql-based-streaming-analytics-flink-job.jar", res => {
                if (res.statusCode != 200) {
                    reject()
                }
                const writeStream = fs.createWriteStream(`${__dirname}/../flinkJob.jar`)
                writeStream.on("finish", () => {
                    writeStream.close();
                    resolve();
                }).on("error", reject);
                res.pipe(writeStream)
            });
        })
    }

}
