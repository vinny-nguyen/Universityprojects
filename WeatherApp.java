package com.vince;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
/*import java.util.List;
import java.util.Map;*/
import java.util.Scanner;

import org.json.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) 

public class WeatherApp {

    private static JSONArray humidityData;

    public static JSONArray getWeatherData(String locationName){
        JSONArray locationData = getLocationData(locationName);
    
        //Extracts latitude and longitude data from their respective keys within JSON object locationData
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        //API call with latitude and longitude from locationData
        String URLString = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,wind_speed_10m&hourly=weather_code,visibility,uv_index&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset&timezone=auto&forecast_days=1&forecast_hours=1";

        try {
            
            HttpURLConnection connection = getAPIResponse(URLString);

            if (connection.getResponseCode() != 200){
                System.out.println("Error: Connection to API was not successful");
                return null;    
            }
            
            //stores result JSON data
            StringBuilder resultJSON = new StringBuilder();
            Scanner jsonScanner = new Scanner(connection.getInputStream());
            while(jsonScanner.hasNext()){
                //scans and stores data into string builder
                resultJSON.append(jsonScanner.nextLine());
            }

            jsonScanner.close();

            connection.disconnect();

            JSONParser jsonParser = new JSONParser();
            JSONObject resultJsonObject = (JSONObject) jsonParser.parse(String.valueOf(resultJSON));
            
            JSONObject current = (JSONObject) resultJsonObject.get("current");
            JSONObject hourly = (JSONObject) resultJsonObject.get("hourly");
            JSONObject daily = (JSONObject) resultJsonObject.get("daily");

            JSONArray time = (JSONArray) current.get("time");
            int index = findCurrentTimeIndex(time);

            JSONArray dayNightData = (JSONArray) current.get("is_day");
            String dayNight = convertWeatherCode((long)dayNightData.get(index));

            JSONArray temperatureData = (JSONArray) current.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            JSONArray feelsLikeTemperatureData = (JSONArray) current.get("apparent_temperature");
            double feelsLikeTemperature = (double) feelsLikeTemperatureData.get(index);

            JSONArray windSpeedData = (JSONArray) current.get("wind_speed_10m");
            double windSpeed = (double) windSpeedData.get(index);

            JSONArray humidityData = (JSONArray) current.get("relative_humidity_2m");
            int humidity = (int) humidityData.get(index);

            JSONArray weatherCode = (JSONArray) hourly.get("weather_code");
            String weatherCondition = convertWeatherCode((long)weatherCode.get(index));

        } catch(Exception e){

            e.printStackTrace();
        }

        return null;
    }

    private static String convertWeatherCode(long l) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'convertWeatherCode'");
    }

    public static JSONArray getLocationData(String locationName){
        //replaces whitespace in location name to follow API's request format
        locationName = locationName.replaceAll(" ", "+");
        
        //Geolocation API with locationName input  
        String locationURL = "https://geocoding-api.open-meteo.com/v1/search?name=" + locationName + "&count=10&language=en&format=json";

        //"https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/" + locationName + "?unitGroup=metric&key=DJ3QF7PFURMPZN66XCFPKE8GF&contentType=json"; 
        
        try {
            HttpURLConnection connection = getAPIResponse(locationURL);
            
            if (connection.getResponseCode() != 200){
                System.out.println("Error: Connection to API was not successful");
                return null;    
            } 
            
            else {
                //stores API results
                StringBuilder resultJSON = new StringBuilder();
                Scanner jsonScanner = new Scanner(connection.getInputStream()); //reads JSON data from API call
                while (jsonScanner.hasNext()){
                    resultJSON.append(jsonScanner.nextLine()); //stores JSON data into resultJSON String
                }

                //close scanner and URL connection to save resources
                jsonScanner.close(); 
                connection.disconnect();
                //creates JSON parser into a JSON object to access JSON data from API ("_insert json property_")
                JSONParser jsonParser = new JSONParser();
                JSONObject resultJSONObject = (JSONObject) ((org.json.simple.parser.JSONParser) jsonParser).parse(String.valueOf(resultJSON));

                //gets list of API generated location data from location name input
                JSONArray locationData = (JSONArray) resultJSONObject.get("results");
                return locationData;

                /*Javascript: Array [] vs Object {} vs JSON "{}" */
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        return null; //couldn't find location
    }

    private static HttpURLConnection getAPIResponse(String locationURL){
        try {
            URL url = new URL(locationURL); //attempts to create connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET"); //sets request method to "get"
            connection.connect(); //connects to our API
            return connection;

        } catch (IOException e){
            e.printStackTrace();
        }
        return null; //incase connection isn't possible
    }

    private static int findCurrentTimeIndex(JSONArray timeList){
        String currentTime = getCurrentTime();

        /*for(int i = 0; i < timeList.size(); i++);
            String time = (String) timeList.get(i);
            if (time.equalsIgnoreCase(currentTime)){
                return i;
            }*/
        return 0;
    }
    
    public static String getCurrentTime(){
        LocalDateTime currentDateTime = LocalDateTime.now();
        
        int check15minute = LocalDateTime.now().getMinute();
        String minute = "";

        /* -- 00 min -- check15minute -- 15 min -- check15minute -- 30 min -- check15minute -- 45 min -- 00
         * API only updates on a 15 minute basis
        */
    //Checks what time it is to round it back to the nearest number in the 15 minute intervals (round backwards not forward because we're getting weather data from the last 15 minute interval)
    if (check15minute == 00 || check15minute > 00 && check15minute < 15 ){ // 00 < mm < 15
        minute = "00";
    }

    else if (check15minute == 15 || check15minute > 15 && check15minute < 30){ // 15 < mm < 30
        minute = "15";
    }

    else if (check15minute == 30 || check15minute > 30 && check15minute < 45){ // 30 < mm < 45
        minute = "30";
    }

    else if (check15minute == 45 || check15minute > 45 && check15minute < 60){ // 45 < mm < 60 (00)
        minute = "45";
    }
        //date format: "2024-06-01T07:45"
        //https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
        DateTimeFormatter apiFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':" + minute + "'");

        String formattedDateTime = currentDateTime.format(apiFormat);

        return formattedDateTime;
    }

    //Converts number weather code
    public static String convertWeatherCode(long weatherCode, long dayNight, String weatherConditionImagePath){
        String weatherCondition = "";
        //clear sky day/night
        if (weatherCode == 0L){
            if (dayNight == 1){
                weatherCondition = "Sunny";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\clear.png";
            }

            else if (dayNight == 0){
                weatherCondition = "Clear";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\clearnight.png";
            }
        }
        
        //mainly sunny/clear
        else if (weatherCode == 1L){
            if (dayNight == 1){
                weatherCondition = "Mostly Sunny"; //"Mainly Sunny" in API weathercode
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\clear.png";
            }

            else if (dayNight == 0){
                weatherCondition = "Mostly Clear"; //""Mainly Clear" in API weathercode
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\clearnight.png";
            }
        }

        //partly cloudy day/night
        else if (weatherCode == 2L){
            if (dayNight == 1){
                weatherCondition = "Partly Cloudy";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\partlycloudy.png";
            }

            else if (dayNight == 0){
                weatherCondition = "Partly Cloudy";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\partlycloudynight.png";
            }
        }

        else if (weatherCode == 3L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Cloudy";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\cloudy.png";
        }

        else if (weatherCode == 45L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Foggy";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\fog.png";
            }
        }

        else if (weatherCode == 48L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Rime Fog";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\fog.png";
            }
        }

        else if (weatherCode == 51L){
            if (dayNight == 1){
                weatherCondition = "Light Drizzle";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\drizzle.png";
            }

            else if (dayNight == 0){
                weatherCondition = "Light Drizzle";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\drizzlenight.png";
            }
        }

        else if (weatherCode == 53L){
            if (dayNight == 1){
                weatherCondition = "Drizzle";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\drizzle.png";
            }

            else if (dayNight == 0){
                weatherCondition = "Drizzle";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\drizzlenight.png";
            }
        }

        else if (weatherCode == 55L){
            if (dayNight == 1){
                weatherCondition = "Heavy Drizzle";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\drizzle.png";
            }

            else if (dayNight == 0){
                weatherCondition = "Heavy Drizzle";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\drizzlenight.png";
            }
        }

        else if (weatherCode == 56L){
            if (dayNight == 1){
                weatherCondition = "Light Freezing Drizzle";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\drizzle.png";
            }

            else if (dayNight == 0){
                weatherCondition = "Light Freezing Drizzle";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\drizzlenight.png";
            }
        }

        else if (weatherCode == 57L){
            if (dayNight == 1){
                weatherCondition = "Freezing Drizzle";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\freezingrain.png";
            }

            else if (dayNight == 0){
                weatherCondition = "Freezing Drizzle";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\freezingrain.png";
            }
        }

        else if (weatherCode == 61L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Light Rain";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\rain.png";
            }
        }

        else if (weatherCode == 63L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Rain";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\rain.png";
            }
        }

        else if (weatherCode == 65L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Heavy Rain";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\heavyrain.png";
            }
        }

        else if (weatherCode == 66L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Light Freezing Rain";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\freezingrain.png";
            }
        }   

        else if (weatherCode == 67L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Freezing Rain";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\freezingrain.png";
            }
        }   

        else if (weatherCode == 71L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Light Snow";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\snow.png";
            }
        }   
            
        else if (weatherCode == 73L || weatherCode == 77L){ //Snow and Snow Grains
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Snow";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\heavysnow.png";
            }
        }   

        else if (weatherCode == 75L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Heavy Snow";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\heavysnow.png";
            }
        }  

        else if (weatherCode == 80L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Light Showers";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\drizzle.png";
            }
        }  

        else if (weatherCode == 81L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Showers";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\rain.png";
            }
        }

        else if (weatherCode == 82L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Showers";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\heavyrain.png";
            }
        }

        else if (weatherCode == 85L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Light Snow Showers";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\heavysnow.png";
            }
        }

        else if (weatherCode == 86L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Snow Showers";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\heavysnow.png";
            }
        }

        else if (weatherCode == 95L && weatherCode == 96L && weatherCode == 99L){
            if (dayNight == 1 || dayNight == 0){
                weatherCondition = "Thunderstorm";
                weatherConditionImagePath = "src\\main\\java\\com\\vince\\assets\\thunderstorm.png";
            }
        }
    }
        return weatherCondition;
    }
}
