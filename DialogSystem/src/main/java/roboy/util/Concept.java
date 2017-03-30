package roboy.util;

import java.util.Map;
import java.util.HashMap;

import roboy.linguistics.Linguistics;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonArray;

public class Concept {
	
	private Map<String, Object> attributes;

	public Concept()
	{
		this.attributes = new HashMap<String, Object>();
	}

	public Concept(Map<String, Object> attrs)
	{
		this.addAttributes(attrs);
	}

	// public Concept(JsonObject _attributes)
	// {
	// 	for (_attributes: attribute) 
	// 	{
	// 	    System.out.println(jsonValue);
	// 	}
	// }
	
	public Concept(String name){
		this();
		attributes.put(Linguistics.NAME, name);
	}

	public void addAttribute(String property, Object value)
	{
		this.attributes.put(property, value);	
	}

	public void addAttributes(Map<String, Object> attrs)
	{
		this.attributes.putAll(attrs);
	}

	public Map<String, Object> getAttributes()
	{
		return this.attributes;
	}
	
	public Object getAttribute(String key){
		return attributes.get(key);
	}

	public String getProperties() 
	{
		String properties = ""; 

		for (Map.Entry<String, Object> attribute : this.getAttributes().entrySet())
		{
		    if (attribute.getKey() != "class" && attribute.getKey() != "id")
		    {
		    	properties +=  "'" + attribute.getKey() + "',";
		    }
		}

		if (properties.charAt(properties.length()-1)==',')
		{
			properties = properties.substring(0, properties.length()-1);
		}

		return properties;

	}

	public String getValues() 
	{
		
		String values = "";

		for (Map.Entry<String, Object> attribute : this.getAttributes().entrySet())
		{
		    if (attribute.getKey() != "class" && attribute.getKey() != "id")
		    {
		    	values += "'" + attribute.getValue().toString() + "',";
		    }
		    
		}

		if (values.charAt(values.length()-1)==',')
		{
			values = values.substring(0, values.length()-1);
		}

		return values;
	}

	// TODO getClass
	// TODO getID

}