/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umuc.controllers;

import edu.umuc.models.School;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import javax.net.ssl.SSLHandshakeException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

class GetDataThread implements Runnable {
	School school;
	Thread t;
	String year;
	String season;
	String sport;
	
	public GetDataThread (School school, String year, String season, String sport){
		this.school = school;
		this.year = year;
		this.season = season;
		this.sport = sport;
	}

        @Override
	public void run() {
		try {
			setSchoolData();
		} catch (IOException e) {
                        System.out.println("Error with school: " + school.getSchoolName());
			e.printStackTrace();
		}
	}

	private void setSchoolData() throws IOException {
		boolean isError;
		int errorCount = 0;

		do {

			try {
				isError = false;
				int winCount = 0;
				int lossCount = 0;
				int pointsDifference = 0;
				int gameCount = 0;
				float averagePointDifference = 0;
				String schoolPath ="";
				String url = "https://www.washingtonpost.com/allmetsports/" + year + "-" + season + "/" + school.getUrlPath() + "/" + sport + "/";

				Document dataPage = Jsoup.connect(url).timeout(60 * 1000).get();  // 60 Seconds
                                String title = dataPage.title();
                                if (title == null || title.startsWith("404")) {
                                    System.out.println("Missing page for school: " + school.getSchoolName() + " using URL: " + url);    
                                } else {
                                    System.out.println(title);
                                }

                                int recordWins = 0;
                                int recordLosses = 0;
                                Elements recordElements = dataPage.getElementsByClass("record");
                                if (recordElements.size() == 1) {
                                    Element record = recordElements.get(0);
                                    String recordText = record.text();
                                    int colonLocation = recordText.indexOf(":");
                                    if (colonLocation > 0) {
                                        recordText = recordText.substring(colonLocation+1);
                                        int dashLocation = recordText.indexOf("-");
                                        String winsText = recordText.substring(0, dashLocation).trim();
                                        String lossesText = recordText.substring(dashLocation+1).trim();
                                        System.out.println(school.getSchoolName() + " - '" + winsText + "', '" + lossesText + "'"); // TODO: Remove this
                                        recordWins = cleanScore(winsText);
                                        recordLosses = cleanScore(lossesText);
                                    }
                                }

				Elements scheduleElements = dataPage.getElementsByClass("weekly-schedule");
				if (scheduleElements.size() > 0) {
					Element schedule = scheduleElements.get(0);
					Elements games = schedule.getElementsByTag("tr");
					for (Element game : games) {
						Elements data = game.getElementsByTag("td");
						if (!data.isEmpty()) {
							Elements linkElements = data.get(1).getElementsByTag("a");
							Element link;
							if (linkElements.size() > 0) {
								link = linkElements.get(0);
								String href = link.attr("href");
								int startLocation = href.indexOf("/", 20)+1;
								int endLocation = href.indexOf("/", startLocation);
								schoolPath = href.substring(startLocation, endLocation);
							} else {
								schoolPath=""; 
							}
							String text = data.get(2).text();
							char winOrLoss = text.trim().charAt(1);
							int dashLocation = text.indexOf("-");

							if ((winOrLoss == 'W' || winOrLoss == 'L') && dashLocation != -1) {
								int score = 0;
								int opponentScore = 0;
								int firstScore = cleanScore(text.substring(3, dashLocation).trim());
								String secondHalf = text.substring(dashLocation+1).trim();
								int spaceLocation = secondHalf.indexOf(" ");
								if (spaceLocation != -1) {
									secondHalf = secondHalf.substring(0, spaceLocation).trim();
								}

								int secondScore = cleanScore(secondHalf);

								if (winOrLoss == 'W') {
									winCount++;
									score = firstScore;
									opponentScore = secondScore;
								} else {
									lossCount++;
									score = secondScore;
									opponentScore = firstScore;
								}
								pointsDifference += score;
								pointsDifference -= opponentScore;
								gameCount++;
								if (schoolPath != null && !schoolPath.isEmpty()) {
									school.addOpponent(schoolPath);
								}
							}
						}
					}

					school.setWins(winCount);
					school.setLosses(lossCount);
                                        
                                        if (recordWins != winCount || recordLosses != lossCount) {
                                            school.setWinLossRecordIncorrect(true);
                                        }
					averagePointDifference = pointsDifference / (float)gameCount;
					school.setAvgPointDifference(averagePointDifference);
				}
			} catch (SocketTimeoutException | SSLHandshakeException | ConnectException e) {
                            isError = true;
                            errorCount++;
			} catch (Exception e) {
                            isError = true;
                            errorCount++;
                            System.out.println("Error with school: " + school.getSchoolName());
                            e.printStackTrace();   
                        }
		} while (isError && errorCount < 3); 

	}
        
       private int cleanScore (String scoreString) {
           String cleanString = scoreString;
           int findRightParentheses = cleanString.indexOf(")");
           if (findRightParentheses > 0) {
               cleanString = cleanString.substring(findRightParentheses + 1);
           }
           return Integer.parseInt(cleanString.trim());
       }
}