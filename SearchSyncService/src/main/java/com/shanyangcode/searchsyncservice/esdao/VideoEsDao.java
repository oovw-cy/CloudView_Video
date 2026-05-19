package com.shanyangcode.searchsyncservice.esdao;


import com.shanyangcode.searchsyncservice.model.es.VideoEs;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Video ES dao
 */
public interface VideoEsDao extends ElasticsearchRepository<VideoEs, Long> {

}