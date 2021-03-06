package com.crypto;

import com.crypto.slack.SlackWebhook;
import com.crypto.utils.StringUtils;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SmartNode {

    /**
     * Logging
     */
    private static final Logger logger = LoggerFactory.getLogger(SmartNode.class);

    /**
     * Url for retrieving smartnode information
     */
    private final String SMARTNODE_URL = "https://smartcash.bitcoiner.me/smartnodes/calculator/";

    /**
     * Timeout in milliseconds
     */
    private final Integer WAIT_IN_MS = 5000;

    /**
     * Initial smart node investment
     */
    private final Double INITIAL_INVESTMENT = 10000.0;

    /**
     * Number of times to retry connecting to URL
     */
    private final Integer RETRY_COUNT = 3;

    /**
     * Text when values are still loading
     */
    private final String LOADING_STRING = "---";

    /**
     * Date of smart nodes release
     */
    private final String LAUNCH_DATE = "02-01-2018";

    public SmartNode() {}

    public void alertCurrentStatus() {
        WebDriver driver = null;

        for (int retryCount=0; retryCount < this.RETRY_COUNT; ++retryCount) {
            try {
                logger.info("Attempt {} to connect", retryCount);

                // Setup Chrome Driver instance
                logger.info("Setting up Selenium chrome instance");
                ChromeDriverManager.getInstance().setup();
                driver = new ChromeDriver();

                // Retrieve HTML document from page, allow time values to load
                logger.info("Parsing HTML document");
                driver.get(this.SMARTNODE_URL);
                Thread.sleep(this.WAIT_IN_MS);
                Document doc = Jsoup.parse(driver.getPageSource());

                // Retrieve relevant HTML elements
                Element currentSmartCashPrice = doc.getElementById("smartcash_usd_price_0");
                Element nodesOnline = doc.getElementById("number_of_smartnodes_input");
                Element smartNodeReward = doc.getElementById("initial_smart_per_day_per_smartnode");

                // Parse elements for details
                String currentPrice = currentSmartCashPrice.text();
                String numberOfNodesOnline = nodesOnline.attr("value");
                String currentSmartNodeReward = smartNodeReward.text();

                // Retry if any values are still loading
                if (currentPrice.equals(this.LOADING_STRING) ||
                        numberOfNodesOnline.equals(this.LOADING_STRING) ||
                        currentSmartNodeReward.equals(this.LOADING_STRING)) {
                    throw new IOException("Values still loading... retry");
                }

                // Determine number of days since launch
                DateTime today = DateTime.now();
                DateTimeFormatter formatter = DateTimeFormat.forPattern("MM-dd-yyyy");
                DateTime launchDate = formatter.parseDateTime(this.LAUNCH_DATE);
                int daysPassed = Days.daysBetween(launchDate, today).getDays();

                // Process details
                Double dailySmartNodeReward = Double.parseDouble(StringUtils.removeNonNumericCharacters(currentSmartNodeReward));
                Double recoupInvestmentTime = Math.round((this.INITIAL_INVESTMENT / dailySmartNodeReward - daysPassed) * 100.0) / 100.0;

                // Build and send slack alert
                logger.info("Creating slack web client");
                SlackWebhook slack = new SlackWebhook("smartcash-information");

                StringBuilder sb = new StringBuilder();

                sb.append("*Current SmartNode price*: ")
                        .append(currentPrice)
                        .append("\n");
                sb.append("*Current SmartNode reward*: ")
                        .append(currentSmartNodeReward)
                        .append("/DAY\n");
                sb.append("*Number of nodes online*: ").append(numberOfNodesOnline).append("\n");

                sb.append("*Time to double our investment*: ").append(recoupInvestmentTime).append(" days");

                logger.info("Sending alert to slack");
                slack.sendMessage(sb.toString());

                logger.info("Shutting down slack web client");
                slack.shutdown();

                return;
            } catch (IOException | InterruptedException ex) {

            }
            finally {
                // Close created chrome instances
                if (driver != null) {
                    logger.info("Closing chrome instance");
                    driver.close();
                }
            }
        }

        // If execution reaches here, then failed 3 times
        logger.error("{} could not be reached or parsed", this.SMARTNODE_URL);
    }
}
