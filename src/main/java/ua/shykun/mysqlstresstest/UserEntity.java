package ua.shykun.mysqlstresstest;

import org.apache.commons.lang3.RandomStringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity(name = "user")
public class UserEntity {

    private static final int EMAIL_LENGTH = 10;

    @Id
    @GeneratedValue
    public long id;

    @Column(name = "email")
    public String email;

    public static UserEntity generateWithDomain(String domain) {
        UserEntity userEntity = new UserEntity();
        userEntity.email = RandomStringUtils.randomAlphanumeric(EMAIL_LENGTH) + "@" + domain;

        return userEntity;
    }
}
