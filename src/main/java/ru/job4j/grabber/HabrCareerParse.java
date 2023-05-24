package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK_FORMAT = "%s/vacancies/java_developer?page=%d";
    private static final int NUM_PAGES = 5;
    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    public static void main(String[] args) throws IOException {
        DateTimeParser dateTimeParser = new HabrCareerDateTimeParser();
        HabrCareerParse parser = new HabrCareerParse(dateTimeParser);
        List<Post> posts = parser.list(PAGE_LINK_FORMAT);
        posts.forEach(System.out::println);
    }

    @Override
    public List<Post> list(String link) throws IOException {
        List<Post> posts = new ArrayList<>();

        for (int page = 1; page <= NUM_PAGES; page++) {
            String pageLink = String.format(link, SOURCE_LINK, page);
            Document document = Jsoup.connect(pageLink).get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                System.out.println("Parsing page: " + titleElement.text());
                Element linkElement = titleElement.child(0);
                String vacancyName = titleElement.text();
                String vacancyLink = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                Element dateTimeElement = row.select(".vacancy-card__date").first();
                String dateTime = dateTimeElement.select("time").attr("datetime");
                LocalDateTime parsedDateTime = dateTimeParser.parse(dateTime);
                String formattedDateTime = parsedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                try {
                    String description = retrieveDescription(vacancyLink);
                    Post post = new Post(0, vacancyName, vacancyLink, description, parsedDateTime);
                    posts.add(post);
                } catch (IOException e) {
                    System.err.printf("Failed to retrieve description for %s: %s%n", vacancyLink, e.getMessage());
                }
            });
        }
        return posts;
    }

    private String retrieveDescription(String link) throws IOException {
        Document document = Jsoup.connect(link).get();
        Element descriptionElement = document.selectFirst(".vacancy-description__text");
        return descriptionElement.text();
    }
}
