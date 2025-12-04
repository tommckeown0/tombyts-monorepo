import fs from "fs/promises";
import path from "path";
import connectDB from "../config/database";
import Movie from "../models/movie-model";
import Subtitle from "../models/subtitle-model";
import mongoose from "mongoose";
import { Config } from "../src/config";

const supportedExtensions = [".mp4", ".mkv", ".avi"]; // Add more if needed
const torrentsDir = Config.torrentsDir;

type ScanMoviesFunction = (directoryPath?: string) => Promise<void>;

(async () => {
	try {
		await connectDB();
		const scanMovies: ScanMoviesFunction = async (
			directoryPath = torrentsDir
		) => {
			try {
				const files = await fs.readdir(directoryPath);

				for (const file of files) {
					const filePath = path.join(directoryPath, file);
					const stats = await fs.stat(filePath);

					if (
						stats.isFile() &&
						supportedExtensions.includes(
							path.extname(file).toLowerCase()
						)
					) {
						const title = path.basename(file, path.extname(file));

						// Correctly calculate relative path from the root torrentsDir
						const relativePath = path.relative(
							torrentsDir,
							filePath
						);

						const normalizedPath = relativePath.replace(/\\/g, "/");

						// Check if the movie already exists in the database
						const existingMovie = await Movie.findOne({ title });
						if (!existingMovie) {
							await Movie.create({ title, path: normalizedPath });
							console.log(
								`Movie "${title}" added to the database.`
							);
						} else {
							console.log(
								`Movie "${title}" already exists in the database.`
							);
						}

						// Look for subtitle files in the same directory
						const movieDir = path.dirname(filePath);
						try {
							const dirFiles = await fs.readdir(movieDir);
							const srtFiles = dirFiles.filter(f =>
								f.toLowerCase().endsWith('.srt')
							);

							for (const srtFile of srtFiles) {
								const srtPath = path.join(movieDir, srtFile);
								const srtRelativePath = path.relative(
									torrentsDir,
									srtPath
								);
								const normalizedSrtPath = srtRelativePath.replace(/\\/g, "/");

								// Extract language from filename (e.g., "movie.en.srt" -> "en")
								// Default to "en" if no language code found
								let language = "en";
								const match = srtFile.match(/\.([a-z]{2,3})\.srt$/i);
								if (match) {
									language = match[1].toLowerCase();
								}

								// Check if subtitle already exists
								const existingSubtitle = await Subtitle.findOne({
									movieTitle: title,
									path: normalizedSrtPath
								});

								if (!existingSubtitle) {
									await Subtitle.create({
										movieTitle: title,
										path: normalizedSrtPath,
										language: language
									});
									console.log(
										`  → Subtitle "${srtFile}" (${language}) linked to "${title}"`
									);
								}
							}
						} catch (err) {
							console.error(`Error scanning subtitles for "${title}":`, err);
						}

					} else if (stats.isDirectory()) {
						await scanMovies(filePath); // Recursively scan subdirectories
					}
				}
			} catch (error) {
				console.error("Error scanning movies:", error);
			}
		};
		await scanMovies();
		//Log how many movies were added to the database
		const movieCount = await Movie.countDocuments();
		const subtitleCount = await Subtitle.countDocuments();
		console.log(
			`Successfully added/verified ${movieCount} movies and ${subtitleCount} subtitles to the database.`
		);
		mongoose.connection.close();
		console.log("Database connection closed.");
	} catch (error) {
		console.error("Error connecting to the database:", error);
		process.exit(1);
	}
})();
