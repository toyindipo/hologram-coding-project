package com.hologramsciences;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVRecord;

import io.atlassian.fugue.Option;

public class CSVRestaurantService {
    private final List<Restaurant> restaurantList;

    /**
     * TODO: Implement Me
     *
     * From the CSVRecord which represents a single line from src/main/resources/rest_hours.csv
     * Write a parser to read the line and create an instance of the Restaurant class (Optionally, using the Option class)
     *
     * Example Line:
     *
     *  "Burger Bar","Mon,Tue,Wed,Thu,Sun|11:00-22:00;Fri,Sat|11:00-0:00"
     *
     *  '|'   separates the list of applicable days from the hours span
     *  ';'   separates groups of (list of applicable days, hours span)
     *
     *  So the above line would be parsed as:
     *
     *  Map<DayOfWeek, OpenHours> m = new HashMap<>();
     *  m.put(MONDAY,    new OpenHours(LocalTime.of(11, 0), LocalTime.of(22, 0)));
     *  m.put(TUESDAY,   new OpenHours(LocalTime.of(11, 0), LocalTime.of(22, 0)));
     *  m.put(WEDNESDAY, new OpenHours(LocalTime.of(11, 0), LocalTime.of(22, 0)));
     *  m.put(THURSDAY,  new OpenHours(LocalTime.of(11, 0), LocalTime.of(22, 0)));
     *  m.put(SUNDAY,    new OpenHours(LocalTime.of(11, 0), LocalTime.of(22, 0)));
     *
     *  m.put(FRIDAY,    new OpenHours(LocalTime.of(11, 0), LocalTime.of(0, 0)));
     *  m.put(SATURDAY,  new OpenHours(LocalTime.of(11, 0), LocalTime.of(0, 0)));
     *
     *  Option.some(new Restaurant("Burger Bar", m))
     *
     * This method returns Option.some(parsedRestaurant),
     *       IF the String name, and Map<DayOfWeek, OpenHours> openHours is found in the CSV,
     *         - assume if both columns are in the CSV then they are both parsable.
     *       AND if all values in openHours have !startTime.equals(endTime)
     *
     * This method returns Option.none() when any of the OpenHours for a given restaurant have the same startTime and endDate
     *
     *
     * NOTE, the getDayOfWeek method should be helpful, and the LocalTime should be parsable by LocalDate.parse
     *
     */
    public static Option<Restaurant> parse(final CSVRecord r) {
        try {
            String name = r.get(0);
            String openhoursString = r.get(1);
            Map<DayOfWeek, Restaurant.OpenHours> openHoursMap = parseOpenHour(openhoursString);
            if (!openHoursMap.isEmpty()) {
                Restaurant restaurant = new Restaurant(name, openHoursMap);
                return Option.some(restaurant);
            }
        } catch (Exception ex) {
            return Option.none();
        }
        return Option.none();
    }

    /**
     * TODO: Implement me, This is a useful helper method
     */
    public static Map<DayOfWeek, Restaurant.OpenHours> parseOpenHour(final String openhoursString) {
        Map<DayOfWeek, Restaurant.OpenHours> openHoursMap = new HashMap<>();
        String[] openHoursArray = openhoursString.split(";");
        for (String openHourGroup: openHoursArray) {
            String[] openHourArray = openHourGroup.split("\\|");
            String[] days = openHourArray[0].split(",");
            String[] times = openHourArray[1].split("-");
            if (times[0].equals(times[1])) return Collections.emptyMap();
            Restaurant.OpenHours openHours = new Restaurant.OpenHours(parseTimeFromString(times[0]),
                    parseTimeFromString(times[1]));
            for (String day: days) {
                openHoursMap.put(getDayOfWeek(day).get(), openHours);
            }
        }
        return openHoursMap;
    }

    private static LocalTime parseTimeFromString(final String timeString) {
        return LocalTime.of(Integer.parseInt(timeString.substring(0, 2)), Integer.parseInt(timeString.substring(3)));
    }

    public CSVRestaurantService() throws IOException {
        this.restaurantList = ResourceLoader.parseOptionCSV("rest_hours.csv", CSVRestaurantService::parse);
    }

    public List<Restaurant> getAllRestaurants() {
        return restaurantList;
    }

    /**
     *
     *  TODO: Implement me
     *
     *  A restaurant is considered open when the OpenHours for the dayOfWeek has:
     *
     *  startTime < localTime   && localTime < endTime
     *
     *  If the open hours are 16:00-20:00  Then
     *
     *  15:59 open = false
     *  16:00 open = false
     *  16:01 open = true
     *  20:00 open = false
     *
     *
     *  If the startTime endTime spans midnight, then consider an endTime up until 5:00 to be part of same DayOfWeek as the startTime
     *
     *  SATURDAY, OpenHours are: 20:00-04:00    SUNDAY, OpenHours are: 10:00-14:00
     *
     *  (SATURDAY, 03:00) => open = false
     *  (SUNDAY, 03:00)   => open = true
     *  (SUNDAY, 05:00)   => open = false
     *
     */
    public List<Restaurant> getOpenRestaurants(final DayOfWeek dayOfWeek, final LocalTime localTime) {
        List<Restaurant> restaurants = restaurantList.stream().filter(r -> openRestaurant(r, dayOfWeek, localTime)).
                collect(Collectors.toList());
        return restaurants;
    }

    private static boolean openRestaurant(final Restaurant restaurant, final DayOfWeek dayOfWeek, final LocalTime localTime) {
        if (restaurant.getOpenHoursMap().containsKey(dayOfWeek)) {
            Restaurant.OpenHours openHours = restaurant.getOpenHoursMap().get(dayOfWeek);
            if (localTime.isAfter(openHours.getStartTime()) && (localTime.isBefore(openHours.getEndTime()))) {
                return true;
            }

            DayOfWeek previousDay = dayOfWeek.minus(1);

            if (restaurant.getOpenHoursMap().containsKey(previousDay)) {
                openHours = restaurant.getOpenHoursMap().get(previousDay);
            }
            if (openHours.spansMidnight()) {
                return (localTime.isBefore(openHours.getEndTime())) &&
                        localTime.isBefore(LocalTime.of(5, 0));
            }
        }
        return false;
    }

    public List<Restaurant> getOpenRestaurantsForLocalDateTime(final LocalDateTime localDateTime) {
        return getOpenRestaurants(localDateTime.getDayOfWeek(), localDateTime.toLocalTime());
    }

    public static Option<DayOfWeek> getDayOfWeek(final String s) {

        if (s.equals("Mon")) {
            return Option.some(DayOfWeek.MONDAY);
        } else if (s.equals("Tue")) {
            return Option.some(DayOfWeek.TUESDAY);
        } else if (s.equals("Wed")) {
            return Option.some(DayOfWeek.WEDNESDAY);
        } else if (s.equals("Thu")) {
            return Option.some(DayOfWeek.THURSDAY);
         } else if (s.equals("Fri")) {
            return Option.some(DayOfWeek.FRIDAY);
        } else if (s.equals("Sat")) {
            return Option.some(DayOfWeek.SATURDAY);
        } else if (s.equals("Sun")) {
            return Option.some(DayOfWeek.SUNDAY);
        } else {
            return Option.none();
        }
    }

    public static <S, T> Function<S, Stream<T>> toStreamFunc(final Function<S, Option<T>> function) {
        return s -> function.apply(s).fold(() -> Stream.empty(), t -> Stream.of(t));
    }

    /**
     * NOTE: Useful for generating the data.sql file in src/main/resources/
     */
    public static void main (final String [] args) throws IOException {
        final CSVRestaurantService csvRestaurantService = new CSVRestaurantService();

        csvRestaurantService.getAllRestaurants().forEach(restaurant -> {

            final String name = restaurant.getName().replaceAll("'", "''");

            System.out.println("INSERT INTO restaurants (name) values ('" + name  + "');");

            restaurant.getOpenHoursMap().entrySet().forEach(entry -> {
                final DayOfWeek dayOfWeek = entry.getKey();
                final LocalTime startTime = entry.getValue().getStartTime();
                final LocalTime endTime   = entry.getValue().getEndTime();

                System.out.println("INSERT INTO open_hours (restaurant_id, day_of_week, start_time_minute_of_day, end_time_minute_of_day) select id, '" + dayOfWeek.toString() + "', " + startTime.get(ChronoField.MINUTE_OF_DAY) + ", " + endTime.get(ChronoField.MINUTE_OF_DAY) + " from restaurants where name = '" + name + "';");

            });
        });
    }
}
