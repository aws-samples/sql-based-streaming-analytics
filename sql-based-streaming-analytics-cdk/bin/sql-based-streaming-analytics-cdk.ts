#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { SqlBasedStreamingAnalyticsKdaStack } from '../lib/sql-based-streaming-analytics-kda-stack';

const app = new cdk.App();
new SqlBasedStreamingAnalyticsKdaStack(app, 'SqlBasedStreamingAnalyticsKdaStack', {});
