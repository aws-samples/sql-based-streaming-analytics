#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import {CoreSolutionStack} from '../lib/core-solution-stack';
import {
    FullSolutionStack
} from "../lib/full-solution-stack";
import {FullSolutionBaseStack} from "../lib/full-solution-base-stack";

async function main() {
    const app = new cdk.App();
    let fullSolutionBaseStack = new FullSolutionBaseStack(app, 'SqlBasedStreamingAnalyticsFullSolutionBaseStack', {});
    let coreSolutionStack = new CoreSolutionStack(app, 'SqlBasedStreamingAnalyticsKdaStack', {
        inputStream: fullSolutionBaseStack.kinesisInputStream,
        outputStream: fullSolutionBaseStack.kinesisOutputStream
    });
    try {
        await coreSolutionStack.startResourceCreation()
    } catch (e) {
        console.error("Error: ", e)
    }
    let fullSolutionStack = new FullSolutionStack(app, 'SqlBasedStreamingAnalyticsElasticBeanstalkStack', {
        inputStream: fullSolutionBaseStack.kinesisInputStream,
        outputStream: fullSolutionBaseStack.kinesisOutputStream,
        msfApplications: coreSolutionStack.msfApplications
    });
    fullSolutionStack.addDependency(coreSolutionStack)
    try {
        await fullSolutionStack.startResourceCreation()
    } catch (e) {
        console.error("Error: ", e)
    }
}

main()
