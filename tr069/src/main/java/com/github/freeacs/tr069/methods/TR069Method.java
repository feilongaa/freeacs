package com.github.freeacs.tr069.methods;

import com.github.freeacs.tr069.Properties;
import java.util.HashMap;
import java.util.Map;

public class TR069Method {
  public static final String EMPTY = "Empty";

  public static final String INFORM = "Inform";

  public static final String GET_PARAMETER_VALUES = "GetParameterValues";

  public static final String SET_PARAMETER_VALUES = "SetParameterValues";

  public static final String TRANSFER_COMPLETE = "TransferComplete";

  public static final String AUTONOMOUS_TRANSFER_COMPLETE = "AutonomousTransferComplete";

  public static final String DOWNLOAD = "Download";

  public static final String FAULT = "Fault";

  public static final String GET_PARAMETER_NAMES = "GetParameterNames";

  public static final String GET_RPC_METHODS_RES = "GetRPCMethodsResponse";

  public static final String REBOOT = "Reboot";

  public static final String FACTORY_RESET = "FactoryReset";

  /** Map of all (SOAP/HTTP-)request actions and what to do next. */
  private final Map<String, HTTPRequestAction> requestMap = new HashMap<>();
  /** Map of all (SOAP/HTTP-)response actions and what to return. */
  private final Map<String, HTTPResponseAction> responseMap = new HashMap<>();
  /** Map of all abbreviations - only used in event-logging. */
  private final Map<String, String> abbrevMap = new HashMap<>();

  public TR069Method(Properties properties) {
    getAbbrevMap().put(EMPTY, "EM");
    getRequestMap()
        .put(
            EMPTY,
            new HTTPRequestAction(
                DoNotProcessReq::process,
                (req) -> EMDecision.process(req, properties.isDiscoveryMode())));
    getResponseMap().put(EMPTY, new HTTPResponseAction(HTTPResponseCreator::buildEM));

    getAbbrevMap().put(GET_PARAMETER_NAMES, "GPN");
    getRequestMap()
        .put(
            GET_PARAMETER_NAMES,
            new HTTPRequestAction(
                (req) -> GPNres.process(req, properties.isDiscoveryMode()),
                makeSimpleDecision(GET_PARAMETER_VALUES)));
    getResponseMap()
        .put(
            GET_PARAMETER_NAMES,
            new HTTPResponseAction(
                reqResData -> HTTPResponseCreator.buildGPN(reqResData, properties)));

    getAbbrevMap().put(INFORM, "IN");
    getRequestMap()
        .put(
            INFORM,
            new HTTPRequestAction(
                (req) -> INreq.process(req, properties.isDiscoveryMode()),
                makeSimpleDecision(INFORM)));
    getResponseMap().put(INFORM, new HTTPResponseAction(HTTPResponseCreator::buildIN));

    getAbbrevMap().put(GET_PARAMETER_VALUES, "GPV");
    getRequestMap()
        .put(
            GET_PARAMETER_VALUES,
            new HTTPRequestAction(
                GPVres::process,
                (req) ->
                    GPVDecision.process(
                        req,
                        properties.isDiscoveryMode(),
                        properties.getPublicUrl(),
                        properties.getConcurrentDownloadLimit())));
    getResponseMap()
        .put(
            GET_PARAMETER_VALUES,
            new HTTPResponseAction(
                reqResData -> HTTPResponseCreator.buildGPV(reqResData, properties)));

    getAbbrevMap().put(SET_PARAMETER_VALUES, "SPV");
    getRequestMap()
        .put(
            SET_PARAMETER_VALUES,
            new HTTPRequestAction(
                SPVres::process, reqResData -> SPVDecision.process(reqResData, properties)));
    getResponseMap()
        .put(
            SET_PARAMETER_VALUES,
            new HTTPResponseAction(
                reqResData -> HTTPResponseCreator.buildSPV(reqResData, properties)));

    getAbbrevMap().put(TRANSFER_COMPLETE, "TC");
    getRequestMap()
        .put(TRANSFER_COMPLETE, new HTTPRequestAction(TCreq::process, TCDecision::process));
    getResponseMap().put(TRANSFER_COMPLETE, new HTTPResponseAction(HTTPResponseCreator::buildTC));

    getAbbrevMap().put(AUTONOMOUS_TRANSFER_COMPLETE, "ATC");
    getRequestMap()
        .put(
            AUTONOMOUS_TRANSFER_COMPLETE,
            new HTTPRequestAction(TCreq::process, ATCDecision::process));
    getResponseMap()
        .put(AUTONOMOUS_TRANSFER_COMPLETE, new HTTPResponseAction(HTTPResponseCreator::buildATC));

    getAbbrevMap().put(DOWNLOAD, "DO");
    getRequestMap().put(DOWNLOAD, new HTTPRequestAction(DOres::process, makeSimpleDecision(EMPTY)));
    getResponseMap()
        .put(
            DOWNLOAD,
            new HTTPResponseAction(
                (req) -> HTTPResponseCreator.buildDO(req, properties.isFileAuthUsed())));

    getAbbrevMap().put(FAULT, "FA");
    getRequestMap().put(FAULT, new HTTPRequestAction(FAres::process, FADecision::process));

    getAbbrevMap().put(REBOOT, "RE");
    getRequestMap().put(REBOOT, new HTTPRequestAction(REres::process, makeSimpleDecision(EMPTY)));
    getResponseMap().put(REBOOT, new HTTPResponseAction(HTTPResponseCreator::buildRE));

    getAbbrevMap().put(FACTORY_RESET, "FR");
    getRequestMap()
        .put(FACTORY_RESET, new HTTPRequestAction(FRres::process, makeSimpleDecision(EMPTY)));
    getResponseMap().put(FACTORY_RESET, new HTTPResponseAction(HTTPResponseCreator::buildFR));
  }

  private HTTPRequestAction.CheckedRequestFunction makeSimpleDecision(String getRpcMethodsRes) {
    return reqRes -> reqRes.getResponseData().setMethod(getRpcMethodsRes);
  }

  public Map<String, HTTPRequestAction> getRequestMap() {
    return requestMap;
  }

  public Map<String, HTTPResponseAction> getResponseMap() {
    return responseMap;
  }

  public Map<String, String> getAbbrevMap() {
    return abbrevMap;
  }
}
