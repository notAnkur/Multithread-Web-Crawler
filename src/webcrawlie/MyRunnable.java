package webcrawlie;

import java.net.URL;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;

/**
 *
 * @author Ankur Anant <hello@ankuranant.dev>
 */
public class MyRunnable implements Runnable {
    private final String url;
    private final JTextArea ta;
    private final JTextArea scrapeTA;
    private static final CrawlerForm crawlerForm = CrawlerForm.getInstance();
    
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private boolean isPinged = false;
    private final int id;
    private final Object pauseLock = new Object();
    
    StringBuffer response;
    HttpURLConnection con;
    
    MyRunnable(String url, int id, JTextArea ta, JTextArea scrapeTA) {
        this.url = url;
        this.id = id;
        this.ta = ta;
        this.scrapeTA = scrapeTA;
        isPinged = false;
    }
    
    @Override
    public void run() {
        
        while (running) {
            synchronized (pauseLock) {
                if (!running) { // may have changed while waiting to
                    // synchronize on pauseLock
                    break;
                }
                if (paused) {
                    try {
                        synchronized (pauseLock) {
                            pauseLock.wait(); // will cause this Thread to block until 
                            // another thread calls pauseLock.notifyAll()
                            // Note that calling wait() will 
                            // relinquish the synchronized lock that this 
                            // thread holds on pauseLock so another thread
                            // can acquire the lock to call notifyAll()
                            // (link with explanation below this code)
                        }
                    } catch (InterruptedException ex) {
                        break;
                    }
                    if (!running) { // running might have changed since we paused
                        break;
                    }
                }
            }
            
            try {
                    System.out.println("pinging");
                    URL urlObj = new URL(url);
                    con = (HttpURLConnection)urlObj.openConnection();
                    con.setRequestMethod("GET");
                    con.setConnectTimeout(5000);
                    con.connect();
                    
                    isPinged = true;

                    // use BR to write website content to the files
                    BufferedReader bReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String in;
                    response = new StringBuffer();

                    in = bReader.readLine();

                    while(in != null) {
                        response.append(in);
                        in = bReader.readLine();
                    }
                    
                    if(!paused) {
                        System.out.println("stop called");
                        stop();
                    }


                } catch(Exception e) {
                    ta.append("Couldn't scrape " + url + "\n");
                    System.out.println("Error: " + e.getMessage());
                    
                    stop();
                    crawlerForm.StopThread(this.id);
                }
        
        }
        
        if(!running) {
            try {
                scrapeTA.append(url + "-------START---------------------------------" + "\n\n");
                scrapeTA.append(response.toString() + "\n");
                scrapeTA.append(url + "-------END---------------------------------" + "\n\n");
                ta.append("Finished scraping " + url + " -> " + con.getResponseCode() + ": " + con.getResponseMessage() + "\n");
                System.out.println("" + con.getResponseCode());
            } catch (IOException ex) {
                Logger.getLogger(MyRunnable.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            crawlerForm.StopThread(this.id);
            
        }
        
    }
    
    public void stop() {
        running = false;
    }

    public void pause() {
        // you may want to throw an IllegalStateException if !running
        paused = true;
    }

    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll(); // Unblocks thread
        }
    }
    
}
