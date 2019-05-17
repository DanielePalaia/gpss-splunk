## Summary
This software is intended to show how it is possible to integrate Splunk with Greenplum database. It exercises splunk java api 
to make searches or to take all logs stored in Splunk and ingest a Greenplum table in order to do some other searches.</br>
The ingestion is done using gpss and gpsscli functionalities of Greenplum.
The software send all the logs on a kafka broker, and we need to start a gpsscli job in order to ingest a Greenplum table </br>

https://gpdb.docs.pivotal.io/5160/greenplum-stream/loading-gpss.html</br>
https://gpdb.docs.pivotal.io/5160/greenplum-stream/ref/gpsscli.html</br>

It uses GPSS to ingest data in Greenplum so it works just for Greenplum 5.16 or above.</br>
There are some initialization phases in order for the software to work. </br></br>

## Prerequisites:
### 1. Bring some data to Splunk: 
I used a DBMS to bring some data into Splunk. I used DBConnect to bring some Greenplum/Postgresql into Splunk. Here you can follow the istructions: </br></br>
https://subscription.packtpub.com/book/big_data_and_business_intelligence/9781788835237/1/ch01lvl1sec18/getting-data-from-databases-using-db-connect</br></br>
The data I put into Splunk are of this type: </br></br>
dashboard=# select * from services limit 5; </br></br>
  id  | version | environment  |     service_level      | company_id | top_service |             name  </br>            
------+---------+--------------+------------------------+------------+-------------+------------------------------ </br>
 2052 |       0 | Production   | DC - Basic             |          7 | NO          | IE-PCAT PRODUCTION-PROD </br>
 2053 |       0 | Unknown-null | DC - Mission Critical  |         45 | NO          | ARCOR SERVERHOUSING </br>
 2054 |       0 | Production   | DC - Business Standard |         37 | NO          | DE-NSS ATOLL-BE DB SYBASE </br>
 2055 |       0 | Unknown-null | DC - Business Standard |         49 | NO          | VFUK INFRASTRUCTURE SECURITY </br>
 2056 |       0 | Unknown-null | DC - Business Standard |         42 | NO          | SHARED KPI MEASURING </br>
</br></br>
Once loaded into splunk it will start to generate events and you will start to see logs like this:
![Screenshot](./images/image1.png)

### 2. Start a kafka broker and create a topic:
The software is gpsscli which is using Kafka. Start a kafka topic where you want and create a topic with the name you want


### 3. Create a Greenplum output table:
I will show you how to bring the same data you put in Splunk back again in Greenplum from the Splunk logs. Create an output table with the same field as the input table. </br>
In the example case we have: </br></br>
CREATE TABLE public.services (</br>
    id bigint NOT NULL,</br>
    version bigint NOT NULL,</br>
    environment character varying(255) NOT NULL,</br>
    service_level character varying(22) NOT NULL,</br>
    company_id bigint NOT NULL,</br>
    top_service character varying(3) NOT NULL,</br>
    name character varying(255) NOT NULL</br>
);</br>

### 4. Start gpss server and gpsscli:
Start gpss server and set a gpsscli job which will listen on the specified broker and topic. In my case for example gpss yaml is the one specified:

DATABASE: dashboard</br>
USER: gpadmin</br>
HOST: localhost</br>
PORT: 5432</br>
KAFKA:</br>
   INPUT:</br>
     SOURCE:</br>
        BROKERS: 172.16.125.1:9092</br>
        TOPIC: zzzzz</br>
     COLUMNS</br>:
        - NAME: jdata</br>
          TYPE: json</br>
     FORMAT: json</br>
     ERROR_LIMIT: 10</br>
   OUTPUT:</br>
     TABLE: services</br>
     MAPPING:</br>
        - NAME: id</br>
          EXPRESSION: (jdata->>'id')::int</br>
        - NAME: version</br>
          EXPRESSION: (jdata->>'version')::int</br>
        - NAME: environment</br>
          EXPRESSION: (jdata->>'environment')::varchar(255)</br>
        - NAME: service_level</br>
          EXPRESSION: (jdata->>'service_level')::varchar(22)</br>
        - NAME: company_id</br>
          EXPRESSION: (jdata->>'company_id')::int</br>
        - NAME: top_service</br>
          EXPRESSION: (jdata->>'top_service')::varchar(3)</br>
        - NAME: name</br>
          EXPRESSION: (jdata->>'name')::varchar(255)</br>

   COMMIT:</br>
     MAX_ROW: 1000</br></br>
     **Note: Messages are posted in Kakfa as Json in order to give the maximum flexibity on what we need and what we don't**</br>     
     
### 5. The software is written in Java so you need a JVM installed as well as Splunk
You need also to create a .splunkrc  in your home directory specifying connection parameters like: </br>  

host=localhost 
#Splunk admin port (default: 8089) </br> 
port=8089   </br> 
#Splunk username   
username=daniele   
#Splunk password   
password=XXXXXX   
#Access scheme (default: https)   
scheme=https  
#Splunk version number   
version=7.2.6   
 </br>
## Running the software:
### 1. Configuration file: </br>  
The software is written in Java. in /target directory in you find the .jar file that can be executed. Also it needs a kafka.properties initialization file.
Here you must specify the following info regarding the kafka broker </br>   

host:localhost  
port:9092  
topic:zzzzz</br>  

### 2. Run the jar </br>  
Run the .jar in this way to search for all logs listed in Splunk (taking just the first 100 elements but you can specify more)</br>  

java -jar splunk-0.0.1-SNAPSHOT.jar "search * | head 100" </br>  

You can also specify different type of searches </br>  

### 3. Look at the messages inserted in the topic </br> 
Information are stored as following (as you can look to the consolle:</br> 

{"_bkt":"main~2~3E1FBD62-AB28-4A3C-93D0-5D509AC308D9", "_cd":"2:13577445", "_serial":"96", "timestamp": "2019-05-16 16:42:40.632", "id":"10331", "version":"0", "environment":"Test", "service_level":"AO-Basic", "company_id":"42", "top_service":"NO", "name":"NAVIGATION (VF360)-TEST", "splunk_server":"Danieles-MBP.station", "index":"main", "source":"dahsboard", "_indextime":"1558017760", "_subsecond":".632", "linecount":"1", "_si":"Danieles-MBP.station,main", "host":"localhost", "_sourcetype":"string", "sourcetype":"string", "_time":"2019-05-16T16:42:40.632+02:00"} to topic: zzzzz</br> 
sending: {"_bkt":"main~2~3E1FBD62-AB28-4A3C-93D0-5D509AC308D9", "_cd":"2:13577439", "_serial":"97", "timestamp": "2019-05-16 16:42:40.632", "id":"10330", "version":"0", "environment":"Test", "service_level":"AO-Basic", "company_id":"2", "top_service":"NO", "name":"MULTIMEDIA MESSAGING SERVICE_TEST", "splunk_server":"Danieles-MBP.station", "index":"main", "source":"dahsboard", "_indextime":"1558017760", "_subsecond":".632", "linecount":"1", "_si":"Danieles-MBP.station,main", "host":"localhost", "_sourcetype":"string", "sourcetype":"string", "_time":"2019-05-16T16:42:40.632+02:00"} to topic: zzzzz</br> 
sending: {"_bkt":"main~2~3E1FBD62-AB28-4A3C-93D0-5D509AC308D9", "_cd":"2:13577431", "_serial":"98", "timestamp": "2019-05-16 16:42:40.632", "id":"10329", "version":"0", "environment":"Production", "service_level":"DC - Business Premium", "company_id":"37", "top_service":"NO", "name":"GSOC-LOCAL SECURITY INCIDENT-POLICY VIOLATION (DE)", "splunk_server":"Danieles-MBP.station", "index":"main", "source":"dahsboard", "_indextime":"1558017760", "_subsecond":".632", "linecount":"1", "_si":"Danieles-MBP.station,main", "host":"localhost", "_sourcetype":"string", "sourcetype":"string", "_time":"2019-05-16T16:42:40.632+02:00"} to topic: zzzzz</br> 
sending: {"_bkt":"main~2~3E1FBD62-AB28-4A3C-93D0-5D509AC308D9", "_cd":"2:13577425", "_serial":"99", "timestamp": "2019-05-16 16:42:40.632", "id":"10328", "version":"0", "environment":"Production", "service_level":"AO-Basic", "company_id":"42", "top_service":"NO", "name":"VODAFONE LIVE PORTAL-TEST", "splunk_server":"Danieles-MBP.station", "index":"main", "source":"dahsboard", "_indextime":"1558017760", "_subsecond":".632", "linecount":"1", "_si":"Danieles-MBP.station,main", "host":"localhost", "_sourcetype":"string", "sourcetype":"string", "_time":"2019-05-16T16:42:40.632+02:00"} to topic: zzzzz</br> 

### 4. Stop the gpsscli job when you want to finalize the writing on Greenplum </br> 

### 5. Have a look to the output table specified: </br> 
Based on the mapping specified on the .yaml file of the gpsscli you should have in output the same entries you submitted in input.

### 6. Try to personalize different output mapping: </br> 
Based on your need you may want to put new fields from splunk like the timpestamp, the splunk server which originated the log ecc... you can extend the .yaml file to add this entries.
Or you may want to store the entries in different way (for example storing all the .json as a type json in Greenplum directly)  </br> 

## Compiling the software:

**If you wish to compile the software you can just mvn install on the root directory** </br>
**Jar file will be produced inside /target**
