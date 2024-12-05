package com.driver;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

@Repository
public class SpotifyRepository {
    public HashMap<Artist, List<Album>> artistAlbumMap;
    public HashMap<Album, List<Song>> albumSongMap;
    public HashMap<Playlist, List<Song>> playlistSongMap;
    public HashMap<Playlist, List<User>> playlistListenerMap;
    public HashMap<User, Playlist> creatorPlaylistMap;
    public HashMap<User, List<Playlist>> userPlaylistMap;
    public HashMap<Song, List<User>> songLikeMap;

    public List<User> users;
    public List<Song> songs;
    public List<Playlist> playlists;
    public List<Album> albums;
    public List<Artist> artists;

    public SpotifyRepository(){
        //To avoid hitting apis multiple times, initialize all the hashmaps here with some dummy data
        artistAlbumMap = new HashMap<>();
        albumSongMap = new HashMap<>();
        playlistSongMap = new HashMap<>();
        playlistListenerMap = new HashMap<>();
        creatorPlaylistMap = new HashMap<>();
        userPlaylistMap = new HashMap<>();
        songLikeMap = new HashMap<>();

        users = new ArrayList<>();
        songs = new ArrayList<>();
        playlists = new ArrayList<>();
        albums = new ArrayList<>();
        artists = new ArrayList<>();
    }

    public User createUser(String name, String mobile) {
        User user = new User(name, mobile);
        users.add(user);
        return user;
    }

    public Artist createArtist(String name) {
        Artist artist = new Artist(name);
        artists.add(artist);
        return artist;
    }

    public Album createAlbum(String title, String artistName) {
        Artist artist = artists.stream()
                .filter(a -> a.getName().equals(artistName))
                .findFirst()
                .orElseGet(() -> createArtist(artistName));

        Album album = new Album(title);
        albums.add(album);
        artistAlbumMap.computeIfAbsent(artist, k -> new ArrayList<>()).add(album);
        return album;
    }

    public Song createSong(String title, String albumName, int length) throws Exception{
        Album album = albums.stream()
                .filter(a -> a.getTitle().equals(albumName))
                .findFirst()
                .orElseThrow(() -> new Exception("Album does not exist"));

        Song song = new Song(title, length);
        songs.add(song);
        albumSongMap.computeIfAbsent(album, k -> new ArrayList<>()).add(song);
        return song;
    }

    public Playlist createPlaylistOnLength(String mobile, String title, int length) throws Exception {
        User user = users.stream()
                .filter(u -> u.getMobile().equals(mobile))
                .findFirst()
                .orElseThrow(() -> new Exception("User does not exist"));

        List<Song> matchingSongs = songs.stream()
                .filter(song -> song.getLength() == length)
                .collect(Collectors.toList());

        Playlist playlist = new Playlist(title);
        playlists.add(playlist);
        playlistSongMap.put(playlist, matchingSongs);
        playlistListenerMap.put(playlist, new ArrayList<>(List.of(user)));
        creatorPlaylistMap.put(user, playlist);
        return playlist;
    }

    public Playlist createPlaylistOnName(String mobile, String title, List<String> songTitles) throws Exception {
        User user = users.stream()
                .filter(u -> u.getMobile().equals(mobile))
                .findFirst()
                .orElseThrow(() -> new Exception("User does not exist"));

        List<Song> matchingSongs = songs.stream()
                .filter(song -> songTitles.contains(song.getTitle()))
                .collect(Collectors.toList());

        Playlist playlist = new Playlist(title);
        playlists.add(playlist);
        playlistSongMap.put(playlist, matchingSongs);
        playlistListenerMap.put(playlist, new ArrayList<>(List.of(user)));
        creatorPlaylistMap.put(user, playlist);
        return playlist;
    }

    public Playlist findPlaylist(String mobile, String playlistTitle) throws Exception {
        User user = users.stream()
                .filter(u -> u.getMobile().equals(mobile))
                .findFirst()
                .orElseThrow(() -> new Exception("User does not exist"));

        Playlist playlist = playlists.stream()
                .filter(p -> p.getTitle().equals(playlistTitle))
                .findFirst()
                .orElseThrow(() -> new Exception("Playlist does not exist"));

        List<User> listeners = playlistListenerMap.get(playlist);
        if (!listeners.contains(user)) {
            listeners.add(user);
        }
        return playlist;
    }

    public Song likeSong(String mobile, String songTitle) throws Exception {
        User user = users.stream()
                .filter(u -> u.getMobile().equals(mobile))
                .findFirst()
                .orElseThrow(() -> new Exception("User does not exist"));

        Song song = songs.stream()
                .filter(s -> s.getTitle().equals(songTitle))
                .findFirst()
                .orElseThrow(() -> new Exception("Song does not exist"));

        List<User> likers = songLikeMap.computeIfAbsent(song, k -> new ArrayList<>());
        if (!likers.contains(user)) {
            likers.add(user);
            song.setLikes(song.getLikes() + 1);

            // Auto-like the artist
            Album album = albumSongMap.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(song))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (album != null) {
                Artist artist = artistAlbumMap.entrySet().stream()
                        .filter(entry -> entry.getValue().contains(album))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);

                if (artist != null) {
                    artist.setLikes(artist.getLikes() + 1);
                }
            }
        }
        return song;
    }

    public String mostPopularArtist() {
        return artists.stream()
                .max(Comparator.comparingInt(Artist::getLikes))
                .map(Artist::getName)
                .orElse(null);
    }

    public String mostPopularSong() {
        return songs.stream()
                .max(Comparator.comparingInt(Song::getLikes))
                .map(Song::getTitle)
                .orElse(null);
    }
}
