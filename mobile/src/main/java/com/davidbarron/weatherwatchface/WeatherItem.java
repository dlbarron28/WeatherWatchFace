package com.davidbarron.weatherwatchface;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class WeatherItem implements Serializable {
    private String city;
    private String lon;
    private String lat;
    private Date rise;
    private Date set;
    private int temp_value;
    private int temp_min;
    private int temp_max;
    private String humidity_value;
    private String humidity_unit;
    private String pressure_value;
    private String pressure_unit;
    private String wind_value;
    private String wind_name;
    private String clouds_name;
    private String weather_value;
    private String weather_icon;
    private Date update;
    private String weather_number;
    private UUID id;
    SimpleDateFormat format = new SimpleDateFormat("hh:mm");
    private Date updateTime;

    public String getUpdateTime() {
        return format.format(updateTime);
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public UUID getId() {
        return id;
    }
    public void setId(UUID i) {
        id = i;
    }
    public String getWeather_number() {
        return weather_number;
    }

    public void setWeather_number(String weather_number) {
        this.weather_number = weather_number;
    }


    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getRise() {
        return format.format(rise);
    }

    public void setRise(Date rise) {
        this.rise = rise;
    }

    public String getSet() {
        return format.format(set);
    }

    public void setSet(Date set) {
        this.set = set;
    }

    public String getTemp_value() {
        return String.valueOf(temp_value);
    }

    public void setTemp_value(int temp_value) {
        this.temp_value = temp_value;
    }

    public String getTemp_min() {
        return String.valueOf(temp_min);
    }

    public void setTemp_min(int temp_min) {
        this.temp_min = temp_min;
    }

    public String getTemp_max() {
        return String.valueOf(temp_max);
    }

    public void setTemp_max(int temp_max) {
        this.temp_max = temp_max;
    }

    public String getHumidity_value() {
        return humidity_value;
    }

    public void setHumidity_value(String humidity_value) {
        this.humidity_value = humidity_value;
    }

    public String getHumidity_unit() {
        return humidity_unit;
    }

    public void setHumidity_unit(String humidity_unit) {
        this.humidity_unit = humidity_unit;
    }

    public String getPressure_value() {
        return pressure_value;
    }

    public void setPressure_value(String pressure_value) {
        this.pressure_value = pressure_value;
    }

    public String getPressure_unit() {
        return pressure_unit;
    }

    public void setPressure_unit(String pressure_unit) {
        this.pressure_unit = pressure_unit;
    }

    public String getWind_value() {
        return wind_value;
    }

    public void setWind_value(String wind_value) {
        this.wind_value = wind_value;
    }

    public String getWind_name() {
        return wind_name;
    }

    public void setWind_name(String wind_name) {
        this.wind_name = wind_name;
    }

    public String getClouds_name() {
        return clouds_name;
    }

    public void setClouds_name(String clouds_name) {
        this.clouds_name = clouds_name;
    }

    public String getWeather_value() {
        return weather_value;
    }

    public void setWeather_value(String weather_value) {
        this.weather_value = weather_value;
    }

    public String getWeather_icon() {
        return weather_icon;
    }

    public void setWeather_icon(String weather_icon) {
        this.weather_icon = weather_icon;
    }

    public String getUpdate() {
        return format.format(update);
    }

    public void setUpdate(Date update) {
        this.update = update;
    }

    public String getLon() {

        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getCity() {

        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
