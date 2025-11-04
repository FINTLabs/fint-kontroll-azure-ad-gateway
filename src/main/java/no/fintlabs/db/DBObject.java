package no.fintlabs.db;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Member;
import java.security.MessageDigest;
import java.sql.Timestamp;

@Setter
@Getter
@RequiredArgsConstructor
public class DBObject extends Object{
    private Timestamp timestamp;
}