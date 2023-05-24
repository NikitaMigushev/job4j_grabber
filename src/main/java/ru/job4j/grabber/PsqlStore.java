package ru.job4j.grabber;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store {
    private Connection cn;

    private Properties config;

    public PsqlStore(Properties config) {
        this.config = config;
        init();
    }

    private void init() {
        try (InputStream in = new FileInputStream("db/liquibase.properties")) {
            config.load(in);
            Class.forName(config.getProperty("driver-class-name"));
            String url = config.getProperty("url");
            String login = config.getProperty("username");
            String password = config.getProperty("password");
            cn = DriverManager.getConnection(url, login, password);
            DatabaseMetaData metaData = cn.getMetaData();
            System.out.println("Connection is successful!");
            System.out.println(metaData.getUserName());
            System.out.println(metaData.getURL());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private PreparedStatement preparedStatement(String query, Object... parameters) throws SQLException {
        PreparedStatement statement = cn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
        return statement;
    }

    private Post retrievePost(ResultSet resultSet) throws SQLException {
        Post post = new Post(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("link"),
                resultSet.getString("text"),
                resultSet.getTimestamp("created").toLocalDateTime()
        );
        return post;
    }

    @Override
    public void save(Post post) {
        String query = "INSERT INTO POST (name, link, text, created) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement statement = preparedStatement(query,
                post.getTitle(),
                post.getLink(),
                post.getDescription(),
                new java.sql.Timestamp(System.currentTimeMillis()))) {
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Post: " + post.getTitle() + " has been saved in database.");
    }

    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        String query = "SELECT * FROM POST";
        try (PreparedStatement statement = preparedStatement(query)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    posts.add(retrievePost(resultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }

    @Override
    public Post findById(int id) {
        Post post = new Post();
        String query = "SELECT * FROM POST WHERE ID = ?";
        try (PreparedStatement statement = preparedStatement(query, id)) {
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                post = retrievePost(resultSet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return post;
    }

    @Override
    public void close() throws SQLException {
        if (cn != null) {
            cn.close();
        }
    }

    public static void main(String[] args) {
        PsqlStore psqlStore = new PsqlStore(new Properties());
        Post post = new Post(1, "test", "test.ru", "test", LocalDateTime.now());
        psqlStore.save(post);
        List<Post> posts = psqlStore.getAll();
        for (Post element : posts) {
            System.out.println(element);
        }
        System.out.println(psqlStore.findById(1));
    }
}
