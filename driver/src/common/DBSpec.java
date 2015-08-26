package common;

import org.w3c.dom.Element;

public class DBSpec {
  private final String id;
  private final String url;
  private final String user;
  private final String password;

  public DBSpec(Element dbElem) {
    this.id = dbElem.getAttribute("id");
    this.url = dbElem.getAttribute("url");

    //Extract the user and password from a url in the form jdbc:oracle:thin:mktg/cc@//dora21:1521/ClaimCtr
    String[] a1 = this.url.split(":");
    String[] a2 = a1[a1.length - 2].split("@");
    String[] a3 = a2[0].split("/");
    this.user = a3[0].toUpperCase();
    this.password = a3[1];
  }

  public String getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }
}
