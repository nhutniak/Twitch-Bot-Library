package me.tyler.twitchbot.teespring;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class Teespring implements Job{

	private static final String URL = "http://teespring.com/twitch/";
	
	private static final String AMOUNT_CLASS = "persistent_timer__order_count";
	private static final String NAME_CLASS = "campaign__name";
	
	private String id;
	private String name;
	private int shirtsSold;
	private ShirtSoldHandler handler;
	
	/**
	 * @param id the id should be the part after the last / in the url
	 * @param handler the handler to handle shirts being sold
	 */
	public Teespring(String id, ShirtSoldHandler handler){
		this.id = id;
		this.handler = handler;
		
		
		
		try{
			
			Document doc = getWebDoc();
			
			shirtsSold = getLiveSoldShirts(doc);
			name = getLiveName(doc);
	
		}catch(Exception e){
			e.printStackTrace();
		}
		
		
	}
	
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * This value is cached and may not be correct
	 * It is updated before calling {@link ShirtSoldHandler#onShirtSold(int, Teespring)}
	 * @return the amount of shirts sold
	 */
	public int getShirtsSold() {
		return shirtsSold;
	}
	
	private Element getSingleElementByClass(Document doc, String className){
		Elements ele = doc.getElementsByClass(className);
		
		if(ele.size() == 0){
			return null;
		}
		
		return ele.get(0);
	}
	
	private int getLiveSoldShirts(Document doc){
		
		Element ele = getSingleElementByClass(doc, AMOUNT_CLASS);
		
		if(ele == null)
			return 0;
		
		return Integer.parseInt(ele.text().split(" ")[0]);
		
	}
	
	private String getLiveName(Document doc){
		return getSingleElementByClass(doc, NAME_CLASS).text();
	}
	
	private Document getWebDoc(){
		Document doc = null;
		
		try {
			doc = Jsoup.connect(URL+id).timeout(5000).userAgent("Mozilla").get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return doc;
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		Document doc = getWebDoc();
		
		int liveAmount = getLiveSoldShirts(doc);
		name = getLiveName(doc);
		
		int amountSold = liveAmount - shirtsSold;
		shirtsSold = liveAmount;
		
		if(amountSold > 0){
			if(handler != null)
				handler.onShirtSold(amountSold, this);
		}
		
	}
	
}
