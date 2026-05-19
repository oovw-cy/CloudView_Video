package com.shanyangcode.searchsyncservice.esdao;


import com.shanyangcode.searchsyncservice.model.es.UserEs;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * User ES dao
 */
public interface UserEsDao extends ElasticsearchRepository<UserEs, Long> {

}