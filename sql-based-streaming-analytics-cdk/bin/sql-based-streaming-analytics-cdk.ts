#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import {SqlBasedStreamingAnalyticsKdaStack} from '../lib/sql-based-streaming-analytics-kda-stack';
import {
    SqlBasedStreamingAnalyticsElasticBeanstalkStack
} from "../lib/sql-based-streaming-analytics-elastic-beanstalk-stack";

async function main() {
    const app = new cdk.App();
    let sqlBasedStreamingAnalyticsKdaStack = new SqlBasedStreamingAnalyticsKdaStack(app, 'SqlBasedStreamingAnalyticsKdaStack', {});
    try {
        await sqlBasedStreamingAnalyticsKdaStack.startResourceCreation()
    } catch (e) {
        console.error("Error: ", e)
    }
    let sqlBasedStreamingAnalyticsElasticBeanstalkStack = new SqlBasedStreamingAnalyticsElasticBeanstalkStack(app, 'SqlBasedStreamingAnalyticsElasticBeanstalkStack', {
        inputStream: sqlBasedStreamingAnalyticsKdaStack.kinesisInputStream,
        outputStream: sqlBasedStreamingAnalyticsKdaStack.kinesisOutputStream,
        msfApplications: sqlBasedStreamingAnalyticsKdaStack.msfApplications
    });
    sqlBasedStreamingAnalyticsElasticBeanstalkStack.addDependency(sqlBasedStreamingAnalyticsKdaStack)
    try {
        await sqlBasedStreamingAnalyticsElasticBeanstalkStack.startResourceCreation()
    } catch (e) {
        console.error("Error: ", e)
    }
}

main()
