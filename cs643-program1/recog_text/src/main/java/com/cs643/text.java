

package com.cs643;


import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.util.*;
import java.io.*;

public class text {

    public static void main(String[] args) {

        String bucketName = "njit-cs-643";
        String queueName = "cars.fifo"; //

        S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();
        RekognitionClient rek = RekognitionClient.builder().region(Region.US_EAST_1).build();
        SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1).build();
        processCarImages(s3, rek, sqs, bucketName, queueName);
    }

    public static void processCarImages(S3Client s3, RekognitionClient rek, SqsClient sqs, String bucketName,
                                        String queueName) {
        boolean q_Exists = false;
        while (!q_Exists) {
            ListQueuesRequest Request_Q_List = ListQueuesRequest.builder()
                    .queueNamePrefix(queueName)
                    .build();
            ListQueuesResponse Response_Q_List = sqs.listQueues(Request_Q_List);
            if (Response_Q_List.queueUrls().size() > 0)
                q_Exists = true;
        }

        // Retrieve the queueURL
        String q_URL = "";
        try {
            GetQueueUrlRequest getReqQ = GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build();
            q_URL = sqs.getQueueUrl(getReqQ)
                    .queueUrl();
        } catch (QueueNameExistsException e) {
            throw e;
        }

        // Process every car images
        try {
            boolean q_End = false;
            HashMap<String, String> outputs = new HashMap<String, String>();

            while (!q_End) {
                // Retrieve next image index
                ReceiveMessageRequest MsgReqRx = ReceiveMessageRequest.builder().queueUrl(q_URL)
                        .maxNumberOfMessages(1).build();
                List<Message> messages = sqs.receiveMessage(MsgReqRx).messages();

                if (messages.size() > 0) {
                    Message message = messages.get(0);
                    String label = message.body();

                    if (label.equals("-1")) {
                        //When instance A terminates its image processing, it adds index -1 to the queue
                        // to signal to instance B that no more indexes will come.
                        q_End = true;
                    } else {
                        System.out.println("Processing: " + label);
                        Image img = Image.builder().s3Object(S3Object.builder().bucket(bucketName).name(label).build())
                                .build();
                        DetectTextRequest request = DetectTextRequest.builder()
                                .image(img)
                                .build();
                        DetectTextResponse result = rek.detectText(request);
                        List<TextDetection> textDetections = result.textDetections();

                        if (textDetections.size() != 0) {
                            String text = "";
                            for (TextDetection textDetection : textDetections) {
                                if (textDetection.type().equals(TextTypes.WORD))
                                    text = text.concat(" " + textDetection.detectedText());
                            }
                            outputs.put(label, text);
                        }
                    }

                    // Delete the message in the queue now that it's been handled
                    DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder().queueUrl(q_URL)
                            .receiptHandle(message.receiptHandle())
                            .build();
                    sqs.deleteMessage(deleteMessageRequest);
                }
            }
            try {
                FileWriter writer = new FileWriter("output.txt");

                Iterator<Map.Entry<String, String>> i = outputs.entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry<String, String> pair = i.next();
                    writer.write(pair.getKey() + ":" + pair.getValue() + "\n");
                    i.remove();
                }

                writer.close();
                System.out.println("Success, results in output.txt");
            } catch (IOException e) {
                System.out.println("Error occurred while processing.");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }
}