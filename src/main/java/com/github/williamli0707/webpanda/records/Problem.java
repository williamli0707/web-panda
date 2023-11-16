package com.github.williamli0707.webpanda.records;

import java.util.LinkedList;

public record Problem(int grade, LinkedList<Attempt> attempts, String defaultCodeTemplate) {
}
