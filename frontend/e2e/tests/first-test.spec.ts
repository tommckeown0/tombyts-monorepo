import { test, Page } from "@playwright/test";
import MoviePage from "../pages/MoviePage";

const username = process.env.USER1;
const password = process.env.PASSWORD1;

if (!username || !password) {
	throw new Error("Please set USER1 and PASSWORD1 environment variables");
}

class LoginPage {
	// Constructor
	constructor(private page: Page) {}

	// Locators
	private readonly loginButton = "data-testid=login-button";
	readonly usernameInput = "data-testid=username-input";
	readonly passwordInput = "data-testid=password-input";
	readonly loginSubmitButton = "data-testid=login-submit-button";
	readonly loginTitle = "data-testid=login-title";

	// Methods
	async login(username: string, password: string) {
		await this.page.locator(this.loginButton).click();
		await this.page.waitForSelector(this.loginTitle);
		// await this.page.locator(this.usernameInput).fill(username);
		await this.page.getByTestId("username-input").fill(username);
		await this.page.locator(this.passwordInput).fill(password);
		await this.page.locator(this.loginSubmitButton).click();
	}
}

test.beforeEach(async ({ page }) => {
	await page.goto("");
});

test("has title", async ({ page }) => {
	// await page.goto("");
	const loginPage = new LoginPage(page);
	await loginPage.login(username, password);

	// await page.locator("data-testid=login-button").click();
	// await page.waitForSelector("data-testid=login-title");
	// await page.locator("data-testid=username-input").fill(username);
	// await page.locator("data-testid=password-input").fill(password);
	// await page.locator("data-testid=login-submit-button").click();

	const moviePage = new MoviePage(page);
	await moviePage.movieHasTitle(/.*Dune.*/);
	await moviePage.clickMovieLink();

	// await page.waitForSelector("data-testid=movie-link-0");
	// await expect(page.locator("data-testid=movie-link-0")).toHaveText(
	// 	/.*Dune.*/
	// );
	// await page.locator("data-testid=movie-link-0").click();

	await page.waitForTimeout(2000);
	// Expect a title "to contain" a substring.
	// await expect(page).toHaveTitle(/Playwright/);
});
