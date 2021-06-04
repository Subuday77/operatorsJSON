package com.operatorsJSON.beans.dbRelated;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.operatorsJSON.beans.Constants.DEFAULTPASSWORDS;

@Entity
@Table(name = "Logins")
@Component
@Scope(value = "prototype")
public class Login {
    private long id;
    private String userName;
    private String password;
    private int accessLevel = 0;
    private long timestamp = System.currentTimeMillis() + 2629800000l;
    private boolean active = true;
    private List<Operator> operators = new ArrayList<>();

    public Login() {
    }

    public Login(long id, String userName, String password, int accessLevel, long timestamp, boolean active, List<Operator> operators) {
        this.id = id;
        this.userName = userName;
        this.password = password;
        this.accessLevel = accessLevel;
        this.timestamp = timestamp;
        this.active = active;
        this.operators = operators;
    }

    public Login(long id, String userName, String password, long timestamp, List<Operator> operators) {
        this.id = id;
        this.userName = userName;
        this.password = password;
        this.timestamp = timestamp;
        this.operators = operators;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", updatable = false, nullable = false)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Column
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Column
    public int getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(int accessLevel) {
        this.accessLevel = accessLevel;
    }

    @Column
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Column
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @OneToMany
    public List<Operator> getOperators() {
        return operators;
    }

    public void setOperators(List<Operator> operators) {
        this.operators = operators;
    }


    @Override
    public String toString() {
        return "Login{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", accessLevel=" + accessLevel +
                ", timestamp=" + timestamp +
                ", active=" + active +
                ", operators=" + operators +
                '}';
    }

    private static String getRandomPassword() {
        Random random = new Random();
        return DEFAULTPASSWORDS[random.ints(1, 0, 251).findFirst().getAsInt()];
    }
}
