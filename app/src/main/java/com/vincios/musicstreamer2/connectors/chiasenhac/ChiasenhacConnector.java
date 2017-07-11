package com.vincios.musicstreamer2.connectors.chiasenhac;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.vincios.musicstreamer2.connectors.Connector;
import com.vincios.musicstreamer2.connectors.ConnectorException;
import com.vincios.musicstreamer2.connectors.Song;
import com.vincios.musicstreamer2.connectors.SongLinkValue;
import com.vincios.musicstreamer2.connectors.chiasenhac.model.ChiasenhacSong;

import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class ChiasenhacConnector implements Connector {

    private static final String SEARCH_LINK = "http://search.chiasenhac.vn/search.php?cat=music&s=";
    public static final String EXTRAS_INFORMATION_PAGE_LINK = "com.vincios.musicstreamer2.connectors.chiasenhac.PAGE_INFORMATION_LINK";
    private static final String LOGTAG = "ChiasenhacConnector";
    private static final String DOWNLOAD_LINK = "http://chiasenhac.vn/download.php?m=";

    public ChiasenhacConnector() {
    }

    @Override
    public List<Song> search(String name, @Nullable String artist, @Nullable Bundle extras) throws ConnectorException {

        String query = name.replace(" ", "+");
        if(artist != null){
            query = query + artist.replace(" ", "+");
        }

        List<ChiasenhacSong> songs = new ArrayList<>();
        try {

            Document document = Jsoup
                    .connect(SEARCH_LINK + query).get();

            Elements ee = document.getElementsByClass("tbtable");

            Element searchList = ee.first();
            Elements serachRows = searchList.getElementsByTag("tr");

            for(int i = 1;i<serachRows.size();i++){

                String id, title, artistTxt, album = "", length, quality;
                int downloads;

                Element tr = serachRows.get(i);
                Elements cols = tr.getElementsByTag("td");
                Element titleBox = cols.get(1);

                Elements titleRows = titleBox.getElementsByTag("p");

                String informationPageLink = titleRows.get(0)
                        .childNodes()
                        .get(0)
                        .attr("href");
                title = titleRows.get(0).text();
                artistTxt = titleRows.get(1).text();
                String[] linkSplitted = informationPageLink.split("~");
                id = linkSplitted[linkSplitted.length-1].replace(".html", "");
                Element qualityBox = cols.get(2);
                String[] lengthquality = qualityBox.child(0).text().split(" ");
                length = lengthquality[0];
                quality = lengthquality[1];

                if(quality.contains("kbps")){
                    quality = quality.substring(0, quality.length() - "kbps".length());
                }

                Element detailsBox = cols.get(4);
                Element detailsP = detailsBox.child(1);
                String ngheplaylist = "Nghe playlist: ";

                for(Element e : detailsP.children()){
                    if(e.tagName().equals("a")){
                        String tmp = e.attr("title");
                        if(!tmp.isEmpty() && tmp.startsWith(ngheplaylist))
                            album = tmp.substring(ngheplaylist.length());
                    }
                }


                Element downloadsBox = cols.get(5);
                downloads = Integer.parseInt(downloadsBox.text().replace(".", ""));

                ChiasenhacSong s = new ChiasenhacSong(id, title, artistTxt, album, quality, length, downloads, informationPageLink);
                songs.add(s);
            }
        }catch (IOException e) {
            throw new ConnectorException(e.getMessage(), e, ConnectorException.ERROR_SERVER_CONNECTION);
        }

        List<ChiasenhacSong> fiteredSongs = filterSongs(songs, name, artist);
        Collections.sort(fiteredSongs, new Comparator<ChiasenhacSong>() {
            @Override
            public int compare(ChiasenhacSong o1, ChiasenhacSong o2) {
                if(o1.getDownloads() > o2.getDownloads()) return -1;
                else if(o1.getDownloads() < o2.getDownloads()) return 1;
                else return 0;
            }
        });

        List<Song> r = new ArrayList<>(fiteredSongs.size());
        for(ChiasenhacSong s : fiteredSongs){
            Log.d(LOGTAG, s.toString());
            r.add(s);
        }

        return r;
    }

    private List<ChiasenhacSong> filterSongs(List<ChiasenhacSong> songs, String titleQuery, String artistQuery){
        List<ChiasenhacSong> r = new ArrayList<>(songs.size());

        for(ChiasenhacSong s : songs){
            String[] queryWords = titleQuery.toLowerCase().split(" ");
            int wordsCount = 0;
            String title = s.getTitle().toLowerCase();

            for(String word : queryWords){
                if (title.contains(word))
                    wordsCount++;
            }

            if(wordsCount > 0)
                r.add(s);
        }

        return r;
    }

    @Override
    public SongLinkValue getLink(Song song, @Nullable Bundle extras) throws ConnectorException {
        String songId = song.getId();
        String quality = song.getBitrate();
        if("Lossless".equalsIgnoreCase(quality)){
            quality = "320";
        }
        String downloadLink = "";
        try {

            Document document = Jsoup.connect(DOWNLOAD_LINK + songId).get();

            String location = document.location();
            System.out.println(document.location());


            String down = location.substring(0, location.length() - ".html".length()) + "_download.html";
            Log.d(LOGTAG, "Download page ="+ down);

            document = 	Jsoup.connect(down).get();

            Element downloadBox = document.getElementById("downloadlink");

            Elements scriptElements = downloadBox.getElementsByTag("script");
            System.out.println(scriptElements.size());
            Element element = scriptElements.get(1);
            DataNode node = element.dataNodes().get(0);
            String data = node.getWholeData();
            String datatrim = data.replaceAll("\t", "");

            BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(datatrim.getBytes("UTF-8"))));
            String sCurrentLine;

            StringBuilder builder = new StringBuilder();

            while ((sCurrentLine = reader.readLine()) != null) {
                String htmlLine;
                if(sCurrentLine.startsWith("document.write('")){
                    htmlLine = sCurrentLine.substring(
                            "document.write('".length(),
                            (sCurrentLine.length() - ("');".length()))
                    );
                    Log.d(LOGTAG, htmlLine);
                    builder.append(htmlLine);
                }
            }


            Document linksBox = Jsoup.parse(builder.toString());
            Elements links = linksBox.getElementsByTag("a");

            Log.d(LOGTAG, "Serch for quality="+quality);
            for(Element link : links){
                if(link.text().contains(quality)){
                    downloadLink = link.attr("href");
                    Log.d(LOGTAG, "Found Link: " + downloadLink);
                    break;
                }
            }

            //System.out.println(datatrim);


        }catch (IOException e) {
            throw new ConnectorException(e.getMessage(), e, ConnectorException.ERROR_SERVER_CONNECTION);
        }

        if(downloadLink.isEmpty())
            throw new ConnectorException("No link found", ConnectorException.RESPONSE_INSUCCESS);

        return new SongLinkValue(songId, downloadLink);
    }

    @Override
    public Song getSong(String songId, @Nullable Bundle extras) {
        return null;
    }
}
