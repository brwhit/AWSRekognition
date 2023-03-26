

package com.cs643;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.Label;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class cars {

    public static void main(String[] args) {

        String bucketName = "njit-cs-643";
        String queueName = "cars.fifo";
        String queueGroup = "group1";

        Region region = Region.US_EAST_1;
        S3Client s3 = S3Client.builder().region(region).build();
        RekognitionClient rek = RekognitionClient.builder().region(region).build();
        SqsClient sqs = SqsClient.builder().region(region).build();

        processBucketImages(s3, rek, sqs, bucketName, queueName, queueGroup);
    }

    public static void processBucketImages(S3Client s3, RekognitionClient rek, SqsClient sqs, String bucketName,
            String queueName, String queueGroup) {

        
        String q_URL = "";
        try {
            ListQueuesRequest Q_Req = ListQueuesRequest.builder()
                    .queueNamePrefix(queueName)
                    .build();
            ListQueuesResponse Q_Res = sqs.listQueues(Q_Req);

            if (Q_Res.queueUrls().size() == 0) {
                Map<String, String> map = new HashMap<>();

                map.put("FifoQueue", "true");
                map.put("ContentBasedDeduplication", "true");
                CreateQueueRequest request = CreateQueueRequest.builder().attributesWithStrings(map).queueName(queueName).build();
                sqs.createQueue(request);

                GetQueueUrlRequest getURLQue = GetQueueUrlRequest.builder().queueName(queueName).build();
                q_URL = sqs.getQueueUrl(getURLQue).queueUrl();
            } else {
                q_URL = Q_Res.queueUrls().get(0);
            }
        } catch (QueueNameExistsException e) {
            throw e;
        }

        // Process the 10 images in the S3 bucket
        try {
            ListObjectsV2Request listObjectsReqManual = ListObjectsV2Request.builder().bucket(bucketName).maxKeys(10)
                    .build();
            ListObjectsV2Response listObjResponse = s3.listObjectsV2(listObjectsReqManual);

            for (S3Object obj : listObjResponse.contents()) {
                System.out.println("Image processed from S3 Bucket, image:  " + obj.key());

                Image img = Image.builder().s3Object(software.amazon.awssdk.services.rekognition.model.S3Object
                        .builder().bucket(bucketName).name(obj.key()).build()).build();
                DetectLabelsRequest request = DetectLabelsRequest.builder().image(img).minConfidence((float) 90)
                        .build();
                DetectLabelsResponse result = rek.detectLabels(request);
                List<Label> labels = result.labels();

                for (Label label : labels) {
                    if (label.name().equals("Car")) {
                        sqs.sendMessage(SendMessageRequest.builder().messageGroupId(queueGroup).queueUrl(q_URL)
                                .messageBody(obj.key()).build());
                        break;
                    }
                }
            }

            // Signal the end of image processing by sending "-1" to the queue
            sqs.sendMessage(SendMessageRequest.builder().queueUrl(q_URL).messageGroupId(queueGroup).messageBody("-1")
                    .build());
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }
}