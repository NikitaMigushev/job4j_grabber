package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HabrCareerParse {
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK_FORMAT = "%s/vacancies/java_developer?page=%d";
    private static final int NUM_PAGES = 5;

    public static void main(String[] args) throws IOException {
        DateTimeParser dateTimeParser = new HabrCareerDateTimeParser();
        for (int page = 1; page <= NUM_PAGES; page++) {
            String pageLink = String.format(PAGE_LINK_FORMAT, SOURCE_LINK, page);
            Connection connection = Jsoup.connect(pageLink);
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                String vacancyName = titleElement.text();
                String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                Element dateTimeElement = row.select(".vacancy-card__date").first();
                String dateTime = dateTimeElement.select("time").attr("datetime");
                LocalDateTime parsedDateTime = dateTimeParser.parse(dateTime);
                String formattedDateTime = parsedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                System.out.printf("%s %s %s%n", vacancyName, link, formattedDateTime);
            });
        }
    }
}
