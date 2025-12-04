import { Page, expect } from "@playwright/test";

class MoviePage {
	// Cosntructor
	constructor(private page: Page) {}

	// Locators
	readonly movieLink = "data-testid=movie-link-0";

	// Methods
	async movieHasTitle(title: RegExp) {
		await this.page.waitForSelector(this.movieLink);
		await expect(this.page.locator(this.movieLink)).toHaveText(title);
	}

	async clickMovieLink() {
		await this.page.waitForSelector(this.movieLink);
		await this.page.locator(this.movieLink).click();
	}
}

export default MoviePage;
