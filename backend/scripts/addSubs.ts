import fs from "fs/promises";
import path from "path";
import connectDB from "../config/database";
import Movie from "../models/movie-model";
import Subtitle from "../models/subtitle-model"; // Import the Subtitle model
import mongoose from "mongoose";
import { Config } from "../src/config";

const torrentsDir = Config.torrentsDir;

type ScanSubtitlesFunction = (directoryPath?: string) => Promise<void>;

(async () => {
	try {
		await connectDB();
		const scanSubtitles: ScanSubtitlesFunction = async (
			directoryPath = torrentsDir
		) => {
			try {
				const files = await fs.readdir(directoryPath);

				for (const file of files) {
					const filePath = path.join(directoryPath, file);
					const stats = await fs.stat(filePath);

					if (
						stats.isFile() &&
						path.extname(file).toLowerCase() === ".srt"
					) {
						const title = path.basename(file, ".srt");

						// Check if a movie with the same title exists
						const movie = await Movie.findOne({ title });

						if (movie) {
							// Correctly calculate relative path from the root torrentsDir
							const relativePath = path.relative(
								torrentsDir,
								filePath
							);

							const normalizedPath = relativePath.replace(
								/\\/g,
								"/"
							);

							// Check if the subtitle already exists
							const existingSubtitle = await Subtitle.findOne({
								movieTitle: title,
								language: "en", // Assuming language is always 'en'
							});

							if (!existingSubtitle) {
								await Subtitle.create({
									movieTitle: title,
									path: normalizedPath,
									language: "en",
								});
								console.log(
									`Subtitle "${title}.srt" added for movie "${title}".`
								);
							} else {
								console.log(
									`Subtitle "${title}.srt" already exists.`
								);
							}
						} else {
							console.log(
								`Movie with title "${title}" not found. Subtitle not added.`
							);
						}
					} else if (stats.isDirectory()) {
						await scanSubtitles(filePath); // Recursively scan subdirectories
					}
				}
			} catch (error) {
				console.error("Error scanning subtitles:", error);
			}
		};

		await scanSubtitles();

		// Log how many subtitles were added
		const subtitleCount = await Subtitle.countDocuments();
		console.log(
			`Successfully added/verified ${subtitleCount} subtitles to the database.`
		);

		mongoose.connection.close();
		console.log("Database connection closed.");
	} catch (error) {
		console.error("Error connecting to the database:", error);
		process.exit(1);
	}
})();
