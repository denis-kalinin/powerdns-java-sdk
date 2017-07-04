package com.itranga.powerdns.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PowerDnsUtils {
	
	private static Pattern URL_PARSER_PATTERN = Pattern.compile("\\{\\/.*?\\}");
	public static final Pattern HOSTNAME_PATTERN = Pattern.compile("^(?<host>[a-zA-Z0-9-]{1,63}|[@\\*])\\.(?<zone>.+)");
	//public static final Pattern HOSTNAME_PATTERN = Pattern.compile("(?=^.{1,253}$)(^(((?!-)[a-zA-Z0-9-]{1,63}(?<!-))|((?!-)[a-zA-Z0-9-]{1,63}(?<!-)\\.)+[a-zA-Z]{2,63})$)");
	//(?=^.{1,253}$)(^(((?!-)[a-zA-Z0-9-]{1,63}(?<!-))|((?!-)[a-zA-Z0-9-]{1,63}(?<!-)\.)+([a-zA-Z]{2,63}))$)
	/**
	 * In PowerDNS API there are URL patterns like <code>/api/v1/servers/localhost/zones{/zone}</code>,
	 * for example. This function will substitute <code>{/zone}</code> with <code>/value</code>.
	 * @param pattern like <code>/api/v1/servers/localhost/zones{/zone}</code>
	 * @param values for matching
	 * @return interpolated string
	 */
	public static String getUrlFromPattern(String pattern, String ... values){
		Matcher matcher = URL_PARSER_PATTERN.matcher(pattern);
		/*
		System.out.println("Match: "+matcher.find());
		for(int i=0; matcher.groupCount() >= i; i++){
			System.out.println("Group "+i+": "+matcher.group(i));
		}
		System.out.println("Named group: " + matcher.group("patt"));
		*/
		int i = 0;
		StringBuffer sb = new StringBuffer();
		while (matcher.find()){
			if(i < values.length){
				String repString = values[i++];
				if (repString != null){
					//System.out.println("Group : " +matcher.group());
					matcher.appendReplacement(sb, "/"+repString);
				}
			} else {
				matcher.appendReplacement(sb, "");
			}
			
		}
		matcher.appendTail(sb);
		String result = sb.toString();
		//System.out.println(result);
		return result;
	}
	/**
	 * Gets DNS <code>zone</code> from provided <code>hostname</code>. For example:
	 * <p><code>getZone("www.example.org") =&gt; "example.org."</code></p>
	 * @param hostname plain (<code>www.example.org</code>) or canonical (<code>www.example.org.</code>),
	 * <code>host</code> part can also be "<code>@</code>" for hostname similar to
	 * zone&mdash;<code>@.example.org.</code>
	 * @return zone name
	 * @throws IllegalArgumentException if <code>hostname</code> is invalid
	 */
	public static String getZone(String hostname){
		if(hostname==null) throw new IllegalArgumentException("hostanme is null");
		Matcher m = HOSTNAME_PATTERN.matcher(hostname);
		if(!m.matches()) throw new IllegalArgumentException(hostname + " - invalid hostanme as in RFC-1123. Exception was thrown by " + PowerDnsUtils.class.getName()+"#getZone(String)");
		String domain = m.group("zone");
		if(!domain.endsWith(".")) domain += ".";
		return domain;
	}
	/**
	 * Gets <code>host</code> from provided <code>hostname</code>. For example:
	 * <p><code>getZone("www.example.org") =&gt; "www"</code></p>
	 * @param hostname plain (<code>www.example.org</code>) or canonical (<code>www.example.org.</code>),
	 * <code>host</code> part can also be "<code>@</code>" for hostname similar to
	 * zone&mdash;<code>@.example.org.</code> or "<code>*</code>" for every unresolved host.
	 * @return host part of hostname
	 * @throws IllegalArgumentException if <code>hostname</code> is invalid
	 */
	public static String getHost(String hostname){
		if(hostname==null) throw new IllegalArgumentException("hostanme is null");
		Matcher m = HOSTNAME_PATTERN.matcher(hostname);
		if(!m.matches()) throw new IllegalArgumentException("hostname is invalid. See " + PowerDnsUtils.class.getName()+"#getZone(String)");
		return m.group("host");
	}
	
	public static String toCanonical(String hostname){
		return hostname.endsWith(".") ? hostname : hostname+".";
	}
}
