package com.davidbarron.weatherwatchface;
/*
http://api.openweathermap.org/data/2.5/weather?q=Cary,NC&mode=xml&units=imperial
    <current>
    <city id="4459467" name="Cary">
    <coord lon="-78.78" lat="35.79"/>
    <country>US</country>
    <sun rise="2014-12-29T12:24:33" set="2014-12-29T22:10:23"/>
    </city>
    <temperature value="51.79" min="51.79" max="51.79" unit="fahrenheit"/>
    <humidity value="100" unit="%"/>
    <pressure value="1023.93" unit="hPa"/>
    <wind>
    <speed value="13.07" name="Strong breeze"/>
    <direction value="48.0055" code="NE" name="NorthEast"/>
    </wind>
    <clouds value="92" name="overcast clouds"/>
    <visibility/>
    <precipitation value="1.5" mode="rain" unit="3h"/>
    <weather number="500" value="light rain" icon="10d"/>
    <lastupdate value="2014-12-29T17:55:40"/>
    </current>

 */

import android.location.Location;
import android.net.Uri;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.UUID;

public class WeatherFetcher {
    private static final String TAG="WeatherFetcher";
    private static final String ENDPOINT="http://api.openweathermap.org/data/2.5/weather";
    private static final String MODE="xml";
    private static final String UNITS="imperial";

    void parseItems(WeatherItem item, XmlPullParser parser) throws XmlPullParserException, IOException,ParseException {
        int eventType = parser.next();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        while(eventType != XmlPullParser.END_DOCUMENT) {
            if(eventType == XmlPullParser.START_TAG) {
                String xmlName = parser.getName();
                switch (xmlName) {
                    case "city":
                        item.setCity(parser.getAttributeValue(null,"name"));
                        break;
                    case "coord":
                        item.setLat(parser.getAttributeValue(null,"lat"));
                        item.setLon(parser.getAttributeValue(null,"lon"));
                        break;
                    case "sun":
                        String time = parser.getAttributeValue(null, "rise");
                        time = time.replace('T',' ');
                        item.setRise(format.parse(time));
                        time = parser.getAttributeValue(null,"set");
                        time = time.replace('T',' ');
                        item.setSet(format.parse(time));
                        break;
                    case "temperature":
                        int temp = Math.round(Float.parseFloat(parser.getAttributeValue(null, "value")));
                        item.setTemp_value(temp);
                        temp = Math.round(Float.parseFloat(parser.getAttributeValue(null,"min")));
                        item.setTemp_min(temp);
                        temp = Math.round(Float.parseFloat(parser.getAttributeValue(null,"max")));
                        item.setTemp_max(temp);
                        break;
                    case "humidity":
                        item.setHumidity_value(parser.getAttributeValue(null,"value"));
                        item.setHumidity_unit(parser.getAttributeValue(null,"unit"));
                        break;
                    case "pressure":
                        item.setPressure_value(parser.getAttributeValue(null,"value"));
                        item.setPressure_unit(parser.getAttributeValue(null,"unit"));
                        break;
                    case "clouds":
                        item.setClouds_name(parser.getAttributeValue(null,"name"));
                        break;
                    case "weather":
                        item.setWeather_icon(parser.getAttributeValue(null,"icon"));
                        item.setWeather_value(parser.getAttributeValue(null,"value"));
                        item.setWeather_number(mapConditionIconToCode(parser.getAttributeValue(null,"icon"),Integer.parseInt(parser.getAttributeValue(null, "number"))));
                        break;
                    case "lastupdate":
                        time = parser.getAttributeValue(null,"value");
                        time = time.replace('T',' ');
                        item.setUpdate(format.parse(time));
                        break;
                    case "speed":
                        item.setWind_name(parser.getAttributeValue(null,"name"));
                        break;
                    case "direction":
                        item.setWind_value(parser.getAttributeValue(null,"name"));
                        break;
                }
            }
            eventType = parser.next();
        }

    }

    public WeatherItem fetchWeather(Location location) {
        WeatherItem item = new WeatherItem();
        item.setId(UUID.randomUUID());
        Log.d(TAG,"Starting fetch for " + location);
        try {
            String url = Uri.parse(ENDPOINT).buildUpon().appendQueryParameter("APPID","82bec7bfaf04817d548862f84d3e1015").appendQueryParameter("mode", MODE).appendQueryParameter("units", UNITS).appendQueryParameter("lat",String.valueOf(location.getLatitude())).appendQueryParameter("lon",String.valueOf(location.getLongitude())).build().toString();
            String xmlString = getUrl(url);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xmlString));
            parseItems(item,parser);
        }
        catch(IOException ex) {
            Log.e(TAG,"Error in fetch:" + ex);
            return null;
        }
        catch(XmlPullParserException ex) {
            Log.e(TAG,"Pull Parser exception: " + ex);
            return null;
        }
        catch(ParseException ex) {
            Log.e(TAG, "Parse Exception: " + ex);
            return null;
        }
        item.setUpdateTime(new Date());
        return item;
    }
    byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            int byteRead = 0;
            byte[] buffer = new byte[1024];
            while ((byteRead = in.read(buffer)) > 0) {
                out.write(buffer,0,byteRead);
            }
            out.close();
            return out.toByteArray();
        }
        finally {
            connection.disconnect();
        }
    }
    public String getUrl(String urlSpec) throws IOException {
        return new String(getUrlBytes((urlSpec)));
    }
    private static final HashMap<String, String> ICON_MAPPING = new HashMap<String, String>();
    static {
        ICON_MAPPING.put("01d", "32");
        ICON_MAPPING.put("01n", "31");
        ICON_MAPPING.put("02d", "30");
        ICON_MAPPING.put("02n", "29");
        ICON_MAPPING.put("03d", "26");
        ICON_MAPPING.put("03n", "26");
        ICON_MAPPING.put("04d", "28");
        ICON_MAPPING.put("04n", "27");
        ICON_MAPPING.put("09d", "12");
        ICON_MAPPING.put("09n", "11");
        ICON_MAPPING.put("10d", "40");
        ICON_MAPPING.put("10n", "45");
        ICON_MAPPING.put("11d", "4");
        ICON_MAPPING.put("11n", "4");
        ICON_MAPPING.put("13d", "16");
        ICON_MAPPING.put("13n", "16");
        ICON_MAPPING.put("50d", "21");
        ICON_MAPPING.put("50n", "20");
    }

    private String mapConditionIconToCode(String icon, int conditionId) {

        // First, use condition ID for specific cases
        switch (conditionId) {
            // Thunderstorms
            case 202:	// thunderstorm with heavy rain
            case 232:	// thunderstorm with heavy drizzle
            case 211:	// thunderstorm
                return "4";
            case 212:	// heavy thunderstorm
                return "3";
            case 221:	// ragged thunderstorm
            case 231:	// thunderstorm with drizzle
            case 201:	// thunderstorm with rain
                return "38";
            case 230:	// thunderstorm with light drizzle
            case 200:	// thunderstorm with light rain
            case 210:	// light thunderstorm
                return "37";

            // Drizzle
            case 300:    // light intensity drizzle
            case 301:	 // drizzle
            case 302:	 // heavy intensity drizzle
            case 310:	 // light intensity drizzle rain
            case 311:	 // drizzle rain
            case 312:	 // heavy intensity drizzle rain
            case 313:	 // shower rain and drizzle
            case 314:	 // heavy shower rain and drizzle
            case 321:    // shower drizzle
                return "9";

            // Rain
            case 500:    // light rain
            case 501:    // moderate rain
            case 520:    // light intensity shower rain
            case 521:    // shower rain
            case 531:    // ragged shower rain
                return "11";
            case 502:    // heavy intensity rain
            case 503:    // very heavy rain
            case 504:    // extreme rain
            case 522:    // heavy intensity shower rain
                return "12";
            case 511:    // freezing rain
                return "10";

            // Snow
            case 600: case 620: return "14"; // light snow
            case 601: case 621: return "16"; // snow
            case 602: case 622: return "41"; // heavy snow
            case 611: case 612:	return "18"; // sleet
            case 615: case 616:	return "5";  // rain and snow

            // Atmosphere
            case 741:    // fog
                return "20";
            case 711:    // smoke
            case 762:    // volcanic ash
                return "22";
            case 701:    // mist
            case 721:    // haze
                return "21";
            case 731:    // sand/dust whirls
            case 751:    // sand
            case 761:    // dust
                return "19";
            case 771:    // squalls
                return "23";
            case 781:    // tornado
                return "0";

            // Extreme
            case 900: return "0";  // tornado
            case 901: return "1";  // tropical storm
            case 902: return "2";  // hurricane
            case 903: return "25"; // cold
            case 904: return "36"; // hot
            case 905: return "24"; // windy
            case 906: return "17"; // hail
        }

        // Not yet handled - Use generic icon mapping
        String condition = ICON_MAPPING.get(icon);
        if (condition != null) {
            return condition;
        }

        return "-1";
    }
}
