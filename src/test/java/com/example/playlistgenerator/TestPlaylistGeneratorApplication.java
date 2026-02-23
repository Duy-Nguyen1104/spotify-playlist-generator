package com.example.playlistgenerator;

import org.springframework.boot.SpringApplication;

public class TestPlaylistGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.from(PlaylistGeneratorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
