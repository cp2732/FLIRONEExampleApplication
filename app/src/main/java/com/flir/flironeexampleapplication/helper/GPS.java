package com.flir.flironeexampleapplication.helper;

/**
 * Adapted by Chris Puda on 7/22/17. Originally created by
 * fabien (http://www.javacms.tech/questions/135470/how-to-save-gps-coordinates-in-exif-data-on-android)
 */
public class GPS {
    private static StringBuilder sb = new StringBuilder(20);

    /**
     * Returns a reference (N for north or S for south) for the latitude.
     *
     * @param latitude The GPS latitude of where an image was taken
     * @return N or S
     */
    public static String latitudeRef(double latitude) {
        return latitude < 0.0d ? "S" : "N";
    }

    /**
     * Returns a reference (N for north or S for south) for the longitude.
     *
     * @param longitude The GPS longitude of where an image was taken
     * @return E or W
     */
    public static String longitudeRef(double longitude) {
        return longitude < 0.0d ? "W" : "E";
    }

    /**
     * convert latitude into DMS (degree minute second) format. For instance<br/>
     * -79.948862 becomes<br/>
     *  79/1,56/1,55903/1000<br/>
     * It works for latitude and longitude<br/>
     * @param latitude could be longitude.
     * @return The string containing the GPS coordinates in a degree-minute-second format
     */
    synchronized public static final String convert(double latitude) {
        latitude = Math.abs(latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        int second = (int) (latitude * 1000.0d);

        sb.setLength(0);
        sb.append(degree);
        sb.append("/1,");
        sb.append(minute);
        sb.append("/1,");
        sb.append(second);
        sb.append("/1000,");
        return sb.toString();
    }
}