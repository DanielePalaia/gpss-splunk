package com.example.splunk;

import com.splunk.*;
import java.io.*;
import java.util.*;

public class SplunkEngine   {

    private Service service;
    private Command command;
    private String query;
    private EntityKafkaBus kafkaClient;


    public SplunkEngine(String []args, String usage, String kafkaIp, String kafkaPort, String kafkaTopic)  {
        command = Command.splunk(usage);
        command.parse(args);

        if (command.args.length != 1)
            Command.error("Search expression required");

        query = command.args[0];
        // Building the kafka proxy
        kafkaClient = new EntityKafkaBus(kafkaIp, kafkaPort, kafkaTopic);

    }

    public void connect()    {
        HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
        service = Service.connect(command.opts);
    }

    private void validateQuery()   {

        // Check the syntax of the query.
        try {
            Args parseArgs = new Args("parse_only", true);
            service.parse(query, parseArgs);
        } catch (HttpException e) {
            String detail = e.getDetail();
            Command.error("query '%s' is invalid: %s", query, detail);
        }

    }


    public void simpleSearch()    throws IOException {

        Args oneshotSearchArgs = new Args();
        oneshotSearchArgs.put("output_mode", "json");

        InputStream stream = service.oneshotSearch(query, oneshotSearchArgs);
        InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
        try {
            OutputStreamWriter writer = new OutputStreamWriter(System.out);
            try {
                int size = 1024;
                char[] buffer = new char[size];
                while (true) {
                    System.out.println("looping");
                    int count = reader.read(buffer);
                    if (count == -1) break;
                    writer.write(buffer, 0, count);
                }

                writer.write("\n");
            } finally {
                writer.close();
            }
        } finally {
            reader.close();
        }

    }

    public void searchEvents()    throws IOException {

        JobResultsArgs resultsArgs = new JobResultsArgs();
        resultsArgs.setOutputMode(JobResultsArgs.OutputMode.JSON);
        Job job = service.getJobs().create(query);

        while (!job.isDone()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        InputStream inpStream = job.getResults(resultsArgs);

        ResultsReaderJson resultsReader = new ResultsReaderJson(inpStream);
        Event event = null;
        while ((event = resultsReader.getNextEvent()) != null) {
            //System.out.println(event.toString());
            kafkaClient.sendJson(event.toString());
        }


    }

}
