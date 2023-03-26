Brad Whitman
CS643-858

Program 1 Instructions

Configuring your EC2 Instances [Steps 1-8] 
1. Log into your AWS Educate account, and launch the AWS Dashboard
2. Search for EC2 in the Services tab
3. Launch a new instance of EC2 from the EC2 Dashboard 
4. Name the instance whatever suits you | Select Amazon Linux for the Image
5. For the security since I was using Putty to launch my instances I created a new key pair as a .ppk file and saved it locally on my computer
6. For the network settings I allowed traffic from SSH | HTTPS | HTTP | and specified only from my IP in the dropdown
7. Set the storage to default
8. Repeat steps 3 - 7 for your second instance as well

Connecting to instances [Steps 8-]
9. In the instances dashboard you will see a column for "Instance ID", click on the first instance
10. Locate the public ipv4 DNS and copy that address [ec2-xxx-xxx-xxx.compute-1.amazonaws.com] and paste it somewhere temporarily (I used the windows notepad)
11. Do the same for your second EC2 instance
12. Launch Putty, and go to Connection >> SSH >> Auth >> Credentials, import your .ppk file from step 5
13. Once the key is imported make two profiles and paste each ipv4 dns into the hostname section
14. Enter your username of instance and connect to the EC2's
15. Run sudo yum update on both instances and install java: sudo yum install java-1.8.0-devel
16. Run these commands on both instances to append both public keys to each EC2 so you can ssh between them
17.     ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa
        cat ~/.ssh/id_rsa.pub
        nano ~/.ssh/authorized_keys
        Append the public keys from the other machine to the end of the file, save and exit.
        ssh <ipv4 dns>

22. Go back to your Canvas dashboard, and in the learner lab click on AWS details, here you will find your access key id and more
23. Copy this page and paste into the temporary notepad
24. On both EC2's do the following: 
24.     $mkdir .aws
        $touch .aws/credentials
        $nano .aws/credentials
        paste the AWS details you copied earlier into this file for both EC2's
25. Create the config file for both 
26.     $touch .aws/config
        $nano .aws/config
        
        [default]
        region=us-east-1
        output=json  

 27. Using WINSCP I placed the jar files onto both EC2's and ran them like so:
 28.        java -jar recog_car.jar  
 29.        java -jar recog_text.jar
 30. View results in output.txt