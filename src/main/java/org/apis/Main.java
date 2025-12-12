package org.apis;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);
        String host;
        String apiKey;

        System.out.println("Enter admin API key");
        apiKey = input.nextLine();
        System.out.println("Enter host with protocol. Example: https://api.swaggerhub.com");
        host = input.nextLine().toLowerCase();
        input.close();
        FetchDocuments(host, apiKey);
    }

    public static void SaveDocumentToFile(String response, String path, String filename){
        new File(path).mkdirs();

        try(FileWriter fileWriter = new FileWriter(path + filename)){
            fileWriter.write(response);
            fileWriter.close();
            System.out.println("File saved to: " + path + filename);
        }
        catch(IOException exception){
            System.out.println("Error writing file: " + exception.getMessage());
        }
    }
    public static void FetchDocuments(String host, String apiKey){

        System.out.println("Fetching all documents...");
        Specs specs = GetSpecs(apiKey, host, 0);

        if (specs == null){
            System.out.println("No documents to save.");
            return;
        }

        HttpClient httpClient = HttpClient.newHttpClient();

        for (int i = 0; i < specs.totalCount; i++){

            Specs spec = GetSpecs(apiKey, host, i);
            String x_versions = spec.apis[0].properties[5].value;
            String[] versions = x_versions.split(",");

            for (String s : versions) {

                String[] URL_split = spec.apis[0].properties[0].url.split("/");
                if (s.charAt(0) == '-' || s.charAt(0) == '*'){
                    s = s.substring(1);
                }
                String URL = String.format("%s//%s/%s/%s/%s/%s/%s",
                        URL_split[0],
                        URL_split[2],
                        URL_split[3],
                        URL_split[4],
                        URL_split[5],
                        URL_split[6],
                        s);
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(URL))
                        .headers(
                                "Accept", "application/yaml",
                                "Authorization", apiKey
                        )
                        .GET()
                        .build();

                HttpResponse<String> response;

                String[] substrings = URL.split("/");
                String documentName = substrings[6];
                String version = substrings[7];
                String organisationName = substrings[5];

                String path = String.format("SwaggerHub-Downloads\\%s\\%s\\",
                        organisationName,
                        substrings[4]);

                String filename = String.format("%s-%s.yaml",
                        documentName,
                        version);
                try {
                    response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200){
                        SaveDocumentToFile(response.body(), path, filename);
                    }
                    else{
                        System.out.println("Something went wrong: " + response.statusCode());
                        System.out.println(response.body());
                        return;
                    }
                } catch (IOException | InterruptedException exception) {
                    System.out.println(exception.getMessage());
                }
            }
        }
        httpClient.close();
        System.out.println("Finished.");

    }
    public static Specs GetSpecs (String apiKey, String host, int page){

        String URL = String.format("%s/v1/specs?specType=ANY&visibility=ANY&state=ALL&page=%d&limit=1&sort=NAME&order=ASC",
                host, page);

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .headers(
                        "Accept", "application/json",
                        "Authorization", apiKey
                )
                .GET()
                .build();

        HttpResponse<String> response;
        Specs apiResponse = new Specs();

        try{
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            Gson gson = new Gson();
            apiResponse = gson.fromJson(response.body(), Specs.class);
            httpClient.close();

        }catch(IOException | InterruptedException exception){
            System.out.println(exception.getMessage());
        }

        return apiResponse;
    }
}