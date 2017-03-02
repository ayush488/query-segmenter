/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.querysegmenter.solr;

import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.AbstractSolrTestCase;
import org.junit.BeforeClass;
import org.junit.Test;

public class QuerySegmenterComponentTest extends AbstractSolrTestCase {

  private static final String DISMAX_QS = "dismax_qs";

  @Override
  public void setUp() throws Exception {
    super.setUp();

    assertU(adoc("id", "1", "project", "Solr", "type", "wiki", "title", "Solr is great!"));
    assertU(adoc("id", "2", "project", "Tika", "type", "wiki", "title", "Solr can be used with Tika"));
    assertU(adoc("id", "3", "project", "Lucene", "type", "wiki", "title", "Locations are fun!", "location", "45.5,-93.5"));

    assertU(adoc("id", "11", "name", "John Smith", "suffix", "Jr", "title", "Solr is great!"));
    assertU(adoc("id", "13", "name", "John Smith", "suffix", "Sr", "title", "Solr can be used with Tika"));
    assertU(adoc("id", "12", "name", "Jon Doe", "suffix", "Jr"));
    assertU(adoc("id", "14", "name", "John Doe"));

    assertU("commit", commit());
  }

  /**
   * The query "solr" should be rewritten to "project:Solr" thus limiting the results to only document 1.
   */
  @Test
  public void test_only_solr_project() {

    ModifiableSolrParams params = new ModifiableSolrParams();
    params.add(CommonParams.QT, DISMAX_QS);
    params.add(CommonParams.Q, "solr");

    SolrQueryRequest req = request(params);

    assertQ(req, "//result[@name='response'][@numFound='1']",
        "//result[@name='response']/doc[1]/str[@name='id'][.='1']");
  }
  
  /**
   * The query "Buffalo" should be rewritten to "location:[46,-93 TO 45,-94]" thus limiting the 
   * results to only document 3.
   */
  @Test
  public void test_area_location() {

    ModifiableSolrParams params = new ModifiableSolrParams();
    params.add(CommonParams.QT, DISMAX_QS);
    params.add(CommonParams.Q, "Buffalo");

    SolrQueryRequest req = request(params);

    assertQ(req, "//result[@name='response'][@numFound='1']",
        "//result[@name='response']/doc[1]/str[@name='id'][.='3']");
  }

  /**
   * The query "John Smith Jr" should be rewritten to "John Smith &bq=suffix:Jr"
   * The response should show three docs name contains John or Smith and the id=11 suffix=Jr will be the first doc as it has higher relevancy
   */
  @Test
  public void test_bq_with_component() {

    ModifiableSolrParams params = new ModifiableSolrParams();
    params.add(CommonParams.QT, "dismax_qs_bq");
    params.add(CommonParams.Q, "John Smith Jr");

    SolrQueryRequest req = request(params, "dismax_qs_bq");

    assertQ(req, "//result[@name='response'][@numFound='3']",
            "//result[@name='response']/doc[1]/str[@name='id'][.='11']",
            "//result[@name='response']/doc[2]/str[@name='id'][.='13']");
  }

  private SolrQueryRequest request(ModifiableSolrParams params) {
    return request(params, DISMAX_QS);
  }

  private SolrQueryRequest request(ModifiableSolrParams params, String handlerName) {
    SolrCore core = h.getCore();
    SolrRequestHandler handler = core.getRequestHandler(handlerName);

    SolrQueryResponse rsp = new SolrQueryResponse();
    NamedList<Object> list = new NamedList<Object>();
    list.add("responseHeader", new SimpleOrderedMap<Object>());
    rsp.setAllValues(list);
    SolrQueryRequest req = new LocalSolrQueryRequest(core, params);
    handler.handleRequest(req, rsp);
    return req;
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore("solrconfig.xml", "schema.xml", "solr");
    //initCore("./solr/collection1/conf/solrconfig.xml", "./solr/collection1/conf/schema.xml");
  }
  
}
