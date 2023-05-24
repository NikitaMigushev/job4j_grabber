package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.quartz.AlertRabbit;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Grabber implements Grab {
    private final Parse parse;
    private final Store store;
    private final Scheduler scheduler;
    private final int time;
    private final Properties config;

    private static final String PAGE_LINK_FORMAT = "%s/vacancies/java_developer?page=%d";

    public Grabber(Parse parse, Store store, Scheduler scheduler, int time) {
        this.parse = parse;
        this.store = store;
        this.scheduler = scheduler;
        this.time = time;
        this.config = readProperties();
    }

    @Override
    public void init() throws SchedulerException {
        JobDataMap data = new JobDataMap();
        data.put("store", store);
        data.put("parse", parse);
        JobDetail job = newJob(GrabJob.class)
                .usingJobData(data)
                .build();
        SimpleScheduleBuilder times = simpleSchedule()
                .withIntervalInSeconds(time)
                .repeatForever();
        Trigger trigger = newTrigger()
                .startNow()
                .withSchedule(times)
                .build();
        scheduler.scheduleJob(job, trigger);
    }

    public static class GrabJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            JobDataMap map = context.getJobDetail().getJobDataMap();
            Store store = (Store) map.get("store");
            Parse parse = (Parse) map.get("parse");
            try {
                List<Post> posts = parse.list(PAGE_LINK_FORMAT);
                for (Post post : posts) {
                    store.save(post);
                }
            } catch (IOException e) {
                throw new JobExecutionException("An error occurred during job execution", e);
            }
        }
    }

    public void web(Store store) {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(Integer.parseInt(config.getProperty("port")))) {
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    OutputStream out = socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "windows-1251"), true);
                    StringBuilder htmlBuilder = new StringBuilder();
                    htmlBuilder.append("<html><body>");
                    htmlBuilder.append("<h1>List Content</h1>");
                    for (Post post : store.getAll()) {
                        htmlBuilder.append("<div>");
                        htmlBuilder.append(String.format("<h2> <a href=\"%s\">", post.getLink())).append(post.getTitle()).append("</a> </h2>");
                        htmlBuilder.append("<p>").append(post.getDescription()).append("</p>");
                        htmlBuilder.append("</div>");
                    }
                    htmlBuilder.append("</body></html>");
                    String htmlResponse = htmlBuilder.toString();
                    writer.println("HTTP/1.1 200 OK");
                    writer.println("Content-Type: text/html; charset=windows-1251");
                    writer.println("Content-Length: " + htmlResponse.getBytes("windows-1251").length);
                    writer.println();
                    writer.println(htmlResponse);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static Properties readProperties() {
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream("db/liquibase.properties")) {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    public static void main(String[] args) throws Exception {
        var cfg = new Properties();
        try (InputStream in = new FileInputStream("db/liquibase.properties")) {
            cfg.load(in);
        }
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        var parse = new HabrCareerParse(new HabrCareerDateTimeParser());
        var store = new PsqlStore(cfg);
        var time = Integer.parseInt(cfg.getProperty("time"));
        Grabber grabber = new Grabber(parse, store, scheduler, time);
        grabber.init();
        grabber.web(new PsqlStore(new Properties()));
    }
}
