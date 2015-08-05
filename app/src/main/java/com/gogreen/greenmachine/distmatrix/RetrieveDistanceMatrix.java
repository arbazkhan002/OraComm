package com.gogreen.greenmachine.distmatrix;

import android.os.AsyncTask;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;

import java.io.IOException;

/**
 * Created by arbkhan on 4/30/2015.
 */
public class RetrieveDistanceMatrix extends AsyncTask<GenericUrl,Result,Result> {
    static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();
    HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
        @Override
        public void initialize(HttpRequest request) {
            request.setParser(new JsonObjectParser(JSON_FACTORY));
        }
    });
    Result distResult;
    @Override
    protected Result doInBackground(GenericUrl... urls){
        try {
            HttpRequest request = requestFactory.buildGetRequest(urls[0]);
            HttpResponse httpResponse = request.execute();
            distResult = httpResponse.parseAs(Result.class);
            return distResult;
        }
        catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }
    @Override
    protected void onPostExecute(Result r) {
        return;
    }

}
