terraform {
  backend "s3" {
    bucket         = "cornalix-tfstate-591859078355"
    key            = "prod/scoring-service/terraform.tfstate"
    region         = "ca-central-1"
    dynamodb_table = "cornalix-tfstate-locks"
    encrypt        = true
  }
}
