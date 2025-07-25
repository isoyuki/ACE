package org.ace.core.server.query;

import java.util.List;

public interface Query {
    void execute();
    int getResultSize();
    List<?> getResultList();
}
