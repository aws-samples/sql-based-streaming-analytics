// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import { S3Event } from 'aws-lambda';
import {
  DescribeApplicationCommand,
  KinesisAnalyticsV2Client,
  ListApplicationsCommand,
  UpdateApplicationCommand,
} from '@aws-sdk/client-kinesis-analytics-v2';
import { PropertyGroup } from '@aws-sdk/client-kinesis-analytics-v2/dist-types/models/models_0';

const msfClient = new KinesisAnalyticsV2Client();

export async function handler(event: S3Event) {
  const msfApplicationList = await msfClient.send(new ListApplicationsCommand({}));
  for (const record of event.Records) {
    const msfApplicationToUpdate = msfApplicationList.ApplicationSummaries?.find((msfApp) => {
      const msfAppName = msfApp.ApplicationName;
      let objectKey = record.s3.object.key.replace('.sql', '');
      return msfAppName?.startsWith(objectKey);
    });
    if (msfApplicationToUpdate) {
      let msfAppDetails = await msfClient.send(
        new DescribeApplicationCommand({
          ApplicationName: msfApplicationToUpdate.ApplicationName,
        })
      );
      let propertyGroups: PropertyGroup[] = [];
      propertyGroups.push({
        PropertyGroupId: 'DEPLOY',
        PropertyMap: {
          'S3-FILE-DATE': record.eventTime,
        },
      });
      let existingPropertyGroups =
        msfAppDetails.ApplicationDetail?.ApplicationConfigurationDescription
          ?.EnvironmentPropertyDescriptions?.PropertyGroupDescriptions;
      if (existingPropertyGroups) {
        for (let existingPropertyGroup of existingPropertyGroups) {
          if (existingPropertyGroup.PropertyGroupId !== 'DEPLOY') {
            propertyGroups.push(existingPropertyGroup);
          }
        }
      }
      await msfClient.send(
        new UpdateApplicationCommand({
          ApplicationName: msfApplicationToUpdate.ApplicationName,
          CurrentApplicationVersionId: msfApplicationToUpdate.ApplicationVersionId,
          ApplicationConfigurationUpdate: {
            EnvironmentPropertyUpdates: {
              PropertyGroups: propertyGroups,
            },
          },
        })
      );
      console.log('Successfully updated MSF application for ' + JSON.stringify(record));
    } else {
      console.log('Found no MSF application for ' + JSON.stringify(record));
    }
  }
  return JSON.stringify({ statusCode: 200 });
}
