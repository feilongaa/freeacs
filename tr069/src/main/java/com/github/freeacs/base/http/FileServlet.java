package com.github.freeacs.base.http;

import com.github.freeacs.base.DownloadLogic;
import com.github.freeacs.base.Log;
import com.github.freeacs.base.db.DBAccess;
import com.github.freeacs.base.db.DBAccessStatic;
import com.github.freeacs.dbi.ACS;
import com.github.freeacs.dbi.File;
import com.github.freeacs.dbi.FileType;
import com.github.freeacs.dbi.Unittype;
import com.github.freeacs.http.AbstractHttpDataWrapper;
import com.github.freeacs.http.HTTPRequestResponseData;
import com.github.freeacs.tr069.Properties;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FileServlet extends AbstractHttpDataWrapper {

  private final String context;

  public FileServlet(DBAccess dbAccess, String context, Properties properties) {
    super(dbAccess, properties);
    this.context = context;
  }

  public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    String firmwareName = null;
    String unittypeName = null;
    OutputStream out = null;
    String authUnittypeName = null;

    try {
      // Create the main object which contains all objects concerning the entire
      // session. This object also contains the SessionData object
      if (properties.isFileAuthUsed()) {
        HTTPRequestResponseData reqRes = getHttpRequestResponseDate(req, res);
        // 2. Authenticate the client (first issue challenge, then authenticate)
        if (!Authenticator.authenticate(reqRes, properties)) {
          return;
        }
        if (reqRes.getSessionData() != null && reqRes.getSessionData().getUnittype() != null) {
          authUnittypeName = reqRes.getSessionData().getUnittype().getName();
        }
      }

      ACS acs = dbAccess.getDBI().getAcs();
      File firmware;
      String pathInfo = req.getPathInfo().substring(this.context.length());
      pathInfo = pathInfo.replaceAll("--", " ");
      String[] pathInfoArr = pathInfo.split("/");
      FileType fileType = FileType.valueOf(pathInfoArr[0]); // Expect it to be a
      // FileType.TYPE_SOFTWARE/TYPE_SCRIPT
      String firmwareVersion = pathInfoArr[1];
      unittypeName = pathInfoArr[2];
      if (authUnittypeName != null && !unittypeName.equals(authUnittypeName)) {
        Log.error(
            FileServlet.class,
            "Requested file in "
                + unittypeName
                + ", but was only authorized for files in "
                + authUnittypeName);
        res.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      Unittype unittype = acs.getUnittype(unittypeName);
      if (unittype == null) {
        Log.error(
            FileServlet.class,
            "Could not find unittype " + unittypeName + " in xAPS, hence file URL is incorrect");
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      firmware = unittype.getFiles().getByVersionType(firmwareVersion, fileType);
      if (firmware == null) {
        Log.error(
            FileServlet.class,
            "Could not find "
                + fileType
                + " version "
                + firmwareVersion
                + " (in unittype "
                + unittype
                + ") in xAPS");
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      firmwareName = firmware.getName();
      Log.debug(
          FileServlet.class,
          "Firmware "
              + firmwareName
              + " exists, will now retrieve binaries for unittype-name "
              + unittypeName);

      out = res.getOutputStream();
      byte[] firmwareImage = DBAccessStatic.readFirmwareImage(firmware);
      if (firmwareImage == null || firmwareImage.length == 0) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        Log.error(
            FileServlet.class,
            "No binaries found for firmware "
                + firmwareName
                + " (in unittype "
                + unittypeName
                + ")");
        return;
      }
      Log.debug(
          FileServlet.class,
          "Binaries found for firmware "
              + firmwareName
              + " (in unittype "
              + unittypeName
              + "), starts to transmit firmware image");
      res.setContentType("application/octet-stream");
      res.setContentLength(firmwareImage.length);
      out.write(firmwareImage);
      Log.debug(
          FileServlet.class,
          "Transmission of firmware " + firmwareName + " (in unittype " + unittypeName + ") ends");
    } catch (Throwable t) {
      Log.error(
          FileServlet.class,
          "Error while retrieving the firmware "
              + firmwareName
              + " (in unittype "
              + unittypeName
              + ")",
          t);
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
    } finally {
      DownloadLogic.removeOldest();
      if (out != null) {
        out.flush();
        out.close();
      }
    }
  }
}
