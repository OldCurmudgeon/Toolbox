/*
 * Copyright 2013 OldCurmudgeon.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oldcurmudgeon.toolbox.xml;

import com.oldcurmudgeon.toolbox.twiddlers.Strings;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Paul
 */
public class XML {

    public static String to(String s) {
        return s.replaceAll("&(?!amp;)", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&apos;");
    }

    public static String from(String s) {
        return s.replaceAll(Pattern.quote("&amp;"), "&")
                .replaceAll(Pattern.quote("&lt;"), "<")
                .replaceAll(Pattern.quote("&gt;"), ">")
                .replaceAll(Pattern.quote("&apos;"), "'")
                .replaceAll(Pattern.quote("&quot;"), "\"");
    }

    public static String decode(String xml) {
        StringBuilder b = new StringBuilder();
        StringBuilder e = new StringBuilder();
        boolean inEntity = false;
        for (int i = 0; i < xml.length(); i++) {
            char ch = xml.charAt(i);
            if (!inEntity) {
                // Not parsing the entity.
                if (ch == '&') {
                    // Start the entity.
                    e.setLength(0);
                    inEntity = true;
                } else {
                    b.append(ch);
                }
            } else {
                // Parsing an entity.
                if (ch == ';' || !Strings.ALPHA.contains(ch)) {
                    if (e.length() > 0) {
                        // Finished! Look it up.
                        String name = e.toString();
                        Integer code;
                        if (name.charAt(0) == '#') {
                            if (name.charAt(1) == 'x') {
                                // Hex
                                code = Integer.parseInt(name.substring(2), 16);
                            } else {
                                // Decimal
                                code = Integer.parseInt(name.substring(1));
                            }
                        } else {
                            code = specials.get(name);
                        }
                        if (code != null) {
                            // Valid.
                            b.append(Character.toChars(code));
                        } else {
                            // Invalid.
                            b.append("&").append(name).append(";");
                        }
                    } else {
                        // & on its own?
                        b.append("&").append(ch);
                    }
                    inEntity = false;
                } else {
                    // Continue the entity.
                    e.append(ch);
                }
            }

        }
        return b.toString();
    }

    public static String hash(CharSequence s) {
        Integer code = specials.get(s.toString());
        if (code != null) {
            return "&#" + code + ";";
        }
        return null;
    }

    public static String unHashAll(CharSequence s) {
        String str = s.toString();
        // Don't do anything if nothing to do.
        if (str.indexOf("&#") >= 0) {
            StringBuilder unHashed = new StringBuilder(s);
            String hashed = new String(unHashed);
            int pos = hashed.indexOf("&#");
            while (pos >= 0) {
                int end = hashed.indexOf(";", pos);
                if (end > pos) {
                    int code;
                    try {
                        code = Integer.parseInt(hashed.substring(pos + 2, end));
                    } catch (NumberFormatException ex) {
                        code = -1;
                    }
                    unHashed.delete(pos, end + 1);
                    if (code >= 0) {
                        unHashed.insert(pos, (char) code);
                    }
                } else {
                    // Remove the & because it is badly formed.
                    unHashed.deleteCharAt(pos);
                }
                hashed = new String(unHashed);
                pos = hashed.indexOf("&#");
            }
            str = unHashed.toString();
        }
        return str;
    }

    public static void main(String[] args) {
        String s = "Hello � &pound; &#123; &#69; &#; &#abc; &#2 ";
        String s2 = unHashAll(s);
        String s3 = unHashAll(s2);
    }

    private static final Map<String, Integer> specials = new HashMap<String, Integer>() {
        {
            // Derived from Wikipedia http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
            put("quot", 34);
            put("amp", 38);
            put("apos", 39);
            put("lt", 60);
            put("gt", 62);
            put("nbsp", 160);
            put("iexcl", 161);
            put("cent", 162);
            put("pound", 163);
            put("curren", 164);
            put("yen", 165);
            put("brvbar", 166);
            put("sect", 167);
            put("uml", 168);
            put("copy", 169);
            put("ordf", 170);
            put("laquo", 171);
            put("not", 172);
            put("shy", 173);
            put("reg", 174);
            put("macr", 175);
            put("deg", 176);
            put("plusmn", 177);
            put("sup2", 178);
            put("sup3", 179);
            put("acute", 180);
            put("micro", 181);
            put("para", 182);
            put("middot", 183);
            put("cedil", 184);
            put("sup1", 185);
            put("ordm", 186);
            put("raquo", 187);
            put("frac14", 188);
            put("frac12", 189);
            put("frac34", 190);
            put("iquest", 191);
            put("Agrave", 192);
            put("Aacute", 193);
            put("Acirc", 194);
            put("Atilde", 195);
            put("Auml", 196);
            put("Aring", 197);
            put("AElig", 198);
            put("Ccedil", 199);
            put("Egrave", 200);
            put("Eacute", 201);
            put("Ecirc", 202);
            put("Euml", 203);
            put("Igrave", 204);
            put("Iacute", 205);
            put("Icirc", 206);
            put("Iuml", 207);
            put("ETH", 208);
            put("Ntilde", 209);
            put("Ograve", 210);
            put("Oacute", 211);
            put("Ocirc", 212);
            put("Otilde", 213);
            put("Ouml", 214);
            put("times", 215);
            put("Oslash", 216);
            put("Ugrave", 217);
            put("Uacute", 218);
            put("Ucirc", 219);
            put("Uuml", 220);
            put("Yacute", 221);
            put("THORN", 222);
            put("szlig", 223);
            put("agrave", 224);
            put("aacute", 225);
            put("acirc", 226);
            put("atilde", 227);
            put("auml", 228);
            put("aring", 229);
            put("aelig", 230);
            put("ccedil", 231);
            put("egrave", 232);
            put("eacute", 233);
            put("ecirc", 234);
            put("euml", 235);
            put("igrave", 236);
            put("iacute", 237);
            put("icirc", 238);
            put("iuml", 239);
            put("eth", 240);
            put("ntilde", 241);
            put("ograve", 242);
            put("oacute", 243);
            put("ocirc", 244);
            put("otilde", 245);
            put("ouml", 246);
            put("divide", 247);
            put("oslash", 248);
            put("ugrave", 249);
            put("uacute", 250);
            put("ucirc", 251);
            put("uuml", 252);
            put("yacute", 253);
            put("thorn", 254);
            put("yuml", 255);
            put("OElig", 338);
            put("oelig", 339);
            put("Scaron", 352);
            put("scaron", 353);
            put("Yuml", 376);
            put("fnof", 402);
            put("circ", 710);
            put("tilde", 732);
            put("Alpha", 913);
            put("Beta", 914);
            put("Gamma", 915);
            put("Delta", 916);
            put("Epsilon", 917);
            put("Zeta", 918);
            put("Eta", 919);
            put("Theta", 920);
            put("Iota", 921);
            put("Kappa", 922);
            put("Lambda", 923);
            put("Mu", 924);
            put("Nu", 925);
            put("Xi", 926);
            put("Omicron", 927);
            put("Pi", 928);
            put("Rho", 929);
            put("Sigma", 931);
            put("Tau", 932);
            put("Upsilon", 933);
            put("Phi", 934);
            put("Chi", 935);
            put("Psi", 936);
            put("Omega", 937);
            put("alpha", 945);
            put("beta", 946);
            put("gamma", 947);
            put("delta", 948);
            put("epsilon", 949);
            put("zeta", 950);
            put("eta", 951);
            put("theta", 952);
            put("iota", 953);
            put("kappa", 954);
            put("lambda", 955);
            put("mu", 956);
            put("nu", 957);
            put("xi", 958);
            put("omicron", 959);
            put("pi", 960);
            put("rho", 961);
            put("sigmaf", 962);
            put("sigma", 963);
            put("tau", 964);
            put("upsilon", 965);
            put("phi", 966);
            put("chi", 967);
            put("psi", 968);
            put("omega", 969);
            put("thetasym", 977);
            put("upsih", 978);
            put("piv", 982);
            put("ensp", 8194);
            put("emsp", 8195);
            put("thinsp", 8201);
            put("zwnj", 8204);
            put("zwj", 8205);
            put("lrm", 8206);
            put("rlm", 8207);
            put("ndash", 8211);
            put("mdash", 8212);
            put("lsquo", 8216);
            put("rsquo", 8217);
            put("sbquo", 8218);
            put("ldquo", 8220);
            put("rdquo", 8221);
            put("bdquo", 8222);
            put("dagger", 8224);
            put("Dagger", 8225);
            put("bull", 8226);
            put("hellip", 8230);
            put("permil", 8240);
            put("prime", 8242);
            put("Prime", 8243);
            put("lsaquo", 8249);
            put("rsaquo", 8250);
            put("oline", 8254);
            put("frasl", 8260);
            put("euro", 8364);
            put("image", 8465);
            put("weierp", 8472);
            put("real", 8476);
            put("trade", 8482);
            put("alefsym", 8501);
            put("larr", 8592);
            put("uarr", 8593);
            put("rarr", 8594);
            put("darr", 8595);
            put("harr", 8596);
            put("crarr", 8629);
            put("lArr", 8656);
            put("uArr", 8657);
            put("rArr", 8658);
            put("dArr", 8659);
            put("hArr", 8660);
            put("forall", 8704);
            put("part", 8706);
            put("exist", 8707);
            put("empty", 8709);
            put("nabla", 8711);
            put("isin", 8712);
            put("notin", 8713);
            put("ni", 8715);
            put("prod", 8719);
            put("sum", 8721);
            put("minus", 8722);
            put("lowast", 8727);
            put("radic", 8730);
            put("prop", 8733);
            put("infin", 8734);
            put("ang", 8736);
            put("and", 8743);
            put("or", 8744);
            put("cap", 8745);
            put("cup", 8746);
            put("int", 8747);
            put("there4", 8756);
            put("sim", 8764);
            put("cong", 8773);
            put("asymp", 8776);
            put("ne", 8800);
            put("equiv", 8801);
            put("le", 8804);
            put("ge", 8805);
            put("sub", 8834);
            put("sup", 8835);
            put("nsub", 8836);
            put("sube", 8838);
            put("supe", 8839);
            put("oplus", 8853);
            put("otimes", 8855);
            put("perp", 8869);
            put("sdot", 8901);
            put("lceil", 8968);
            put("rceil", 8969);
            put("lfloor", 8970);
            put("rfloor", 8971);
            put("lang", 10216);
            put("rang", 10217);
            put("loz", 9674);
            put("spades", 9824);
            put("clubs", 9827);
            put("hearts", 9829);
            put("diams", 9830);
        }
    };
}
